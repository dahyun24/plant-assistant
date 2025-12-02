package cse.plant_care_chatbot.plant.service;

import cse.plant_care_chatbot.plant.dto.SensorComparisonRes;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusService {

    // Config에서 등록한 Bean 주입 (final 필수)
    private final MilvusServiceClient milvusClient;

    @Value("${milvus.collection-name}")
    private String collectionName;

    // 성장 단계 랭크 (DB 값과 정확히 일치해야 함: DIE, Low, Medium, High)
    private final Map<String, Integer> GROWTH_RANK = Map.of(
            "DIE", 0,
            "Low", 1,
            "Medium", 2,
            "High", 3
    );

    private final String[] SENSOR_KEYS = {
            "AirTemperature", "AirHumidity", "Co2", "Quantum",
            "HighSoilTemp", "HighSoilHumi", "LowSoilTemp", "LowSoilHumi"
    };

    /**
     * 메인 분석 메서드 (Step 2)
     */
    public Map<String, Object> searchAndAnalyze(String plantName, String growthLevel, List<Float> queryVector) {
        // 1. 유사 식물 검색 (Top 5)
        List<Map<String, Object>> similarPlants = searchSimilar(queryVector, plantName, 5);

        // 2. 성장 단계별 그룹 검색 (Better / Worse)
        Map<String, List<Map<String, Object>>> groups = searchGroups(plantName, growthLevel, 5L);

        // 3. 결과 조합 (이미지 & 센서 분석)
        List<String> topImages = similarPlants.stream()
                .limit(3)
                .map(p -> (String) p.get("image_name"))
                .collect(Collectors.toList());

        List<SensorComparisonRes> analysis = compareSensors(similarPlants, groups);

        return Map.of("images", topImages, "analysis", analysis);
    }

    // =================================================================
    // 🔍 STEP 1: 유사 식물 검색 (Vector Search)
    // =================================================================
    private List<Map<String, Object>> searchSimilar(List<Float> vector, String plantName, int topK) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.COSINE)
                .withOutFields(Arrays.asList("image_name", "sensor_vector")) // 필요한 필드만 가져오기
                .withTopK(topK)
                .withVectors(Collections.singletonList(vector))
                .withVectorFieldName("text_vector")
                .withExpr(String.format("plant_name == '%s'", plantName)) // 같은 식물 종 내에서만 검색
                .withParams("{\"nprobe\": 32}")
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        handleMilvusError(response);

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());

        // 검색 결과가 없는 경우 빈 리스트 반환
        if (wrapper.getIDScore(0).isEmpty()) {
            return new ArrayList<>();
        }

        return wrapper.getIDScore(0).stream().map(score -> {
            Map<String, Object> m = new HashMap<>();
            m.put("image_name", score.get("image_name"));
            m.put("sensor_vector", score.get("sensor_vector"));
            return m;
        }).collect(Collectors.toList());
    }

    // =================================================================
    // 🔍 STEP 2: 성장 단계별 그룹 검색 (Scalar Query)
    // =================================================================
    private Map<String, List<Map<String, Object>>> searchGroups(String plantName, String currentLevel, Long limit) {
        int currentRank = GROWTH_RANK.getOrDefault(currentLevel, -1);

        // 1. 더 잘 자란 그룹 (Better): 현재 등급보다 높은 랭크 필터링
        List<String> betterLevels = GROWTH_RANK.entrySet().stream()
                .filter(e -> e.getValue() > currentRank)
                .map(Map.Entry::getKey)
                .toList();

        // 2. 더 못 자란 그룹 (Worse): 현재 등급보다 낮은 랭크 필터링
        List<String> worseLevels = GROWTH_RANK.entrySet().stream()
                .filter(e -> e.getValue() < currentRank)
                .map(Map.Entry::getKey)
                .toList();

        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("better", queryByLevels(plantName, betterLevels, limit));
        result.put("worse", queryByLevels(plantName, worseLevels, limit));

        return result;
    }

    // 특정 레벨 리스트에 해당하는 식물들의 센서 데이터 조회
    private List<Map<String, Object>> queryByLevels(String plantName, List<String> levels, Long limit) {
        if (levels.isEmpty()) return Collections.emptyList();

        // Query 표현식 생성: (growth_level == 'A' or growth_level == 'B')
        String levelExpr = levels.stream()
                .map(l -> String.format("growth_level == '%s'", l))
                .collect(Collectors.joining(" or "));

        String expr = String.format("plant_name == '%s' and (%s)", plantName, levelExpr);

        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .withOutFields(Collections.singletonList("sensor_vector")) // 센서값만 필요
                .withLimit(limit)
                .build();

        R<QueryResults> response = milvusClient.query(queryParam);
        handleMilvusError(response);

        QueryResultsWrapper wrapper = new QueryResultsWrapper(response.getData());
        List<Map<String, Object>> list = new ArrayList<>();

        for (long i = 0; i < wrapper.getRowCount(); i++) {
            Map<String, Object> map = new HashMap<>();
            // 센서 벡터 추출
            List<?> vector = (List<?>) wrapper.getFieldWrapper("sensor_vector").getFieldData().get((int) i);
            map.put("sensor_vector", vector);
            list.add(map);
        }
        return list;
    }

    // =================================================================
    // 📊 STEP 3: 센서 데이터 비교 및 조언 생성
    // =================================================================
    private List<SensorComparisonRes> compareSensors(List<Map<String, Object>> similar, Map<String, List<Map<String, Object>>> groups) {
        List<Float> simAvg = calcAvg(similar);
        List<Float> betterAvg = calcAvg(groups.get("better"));
        List<Float> worseAvg = calcAvg(groups.get("worse"));

        List<SensorComparisonRes> result = new ArrayList<>();

        // 데이터가 하나도 없으면 빈 리스트 반환
        if (simAvg == null && betterAvg == null && worseAvg == null) {
            return result;
        }

        for (int i = 0; i < SENSOR_KEYS.length; i++) {
            // 소수점 2자리 반올림
            Double s = (simAvg != null) ? (double) Math.round(simAvg.get(i) * 100) / 100.0 : null;
            Double b = (betterAvg != null) ? (double) Math.round(betterAvg.get(i) * 100) / 100.0 : null;
            Double w = (worseAvg != null) ? (double) Math.round(worseAvg.get(i) * 100) / 100.0 : null;

            result.add(new SensorComparisonRes(SENSOR_KEYS[i], s, b, w));
        }
        return result;
    }

    // 벡터 리스트의 평균 계산
    private List<Float> calcAvg(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) return null;

        // 첫 번째 데이터로 차원 확인 (8차원)
        List<?> firstVec = (List<?>) list.get(0).get("sensor_vector");
        int dim = firstVec.size();
        float[] sum = new float[dim];

        for (Map<String, Object> m : list) {
            List<Float> v = (List<Float>) m.get("sensor_vector");
            for (int i = 0; i < dim; i++) {
                sum[i] += v.get(i);
            }
        }

        List<Float> avg = new ArrayList<>();
        for (float f : sum) {
            avg.add(f / list.size());
        }
        return avg;
    }

    // Milvus 에러 핸들링
    private void handleMilvusError(R<?> response) {
        if (response.getStatus() != R.Status.Success.getCode()) {
            log.error("Milvus Error: {}", response.getMessage());
            throw new RuntimeException("Milvus Operation Failed: " + response.getMessage());
        }
    }
}