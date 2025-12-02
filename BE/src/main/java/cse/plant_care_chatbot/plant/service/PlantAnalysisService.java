package cse.plant_care_chatbot.plant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cse.plant_care_chatbot.plant.dto.PlantReportRes;
import cse.plant_care_chatbot.plant.dto.SensorComparisonRes;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantAnalysisService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper;

    /**
     * Step 3: 최종 리포트 생성 (점수 계산 + LLM 조언)
     */
    public PlantReportRes generateReport(String plantName, String growthLevel, String caption,
                                         List<String> similarImages, List<SensorComparisonRes> sensorData) {

        // 1. 점수 계산 (Metric Scores)
        List<PlantReportRes.MetricScore> metricScores = calculateMetricScores(sensorData);

        // 2. 전체 점수 계산 (Overall Score)
        int overallScore = calculateOverallScore(growthLevel, metricScores);

        // 3. 편차가 큰 Top 3 센서 찾기
        List<SensorComparisonRes> topIssues = findTopIssues(sensorData);

        // 4. Gemini에게 조언 요청 (종합 분석 + 세부 가이드)
        Map<String, String> llmAdvice = askGeminiForAdvice(plantName, growthLevel, caption, sensorData, topIssues);

        return new PlantReportRes(
                plantName,
                overallScore,
                metricScores,
                caption,
                llmAdvice.getOrDefault("analysis", "분석을 생성할 수 없습니다."),
                llmAdvice.getOrDefault("careGuide", "가이드를 생성할 수 없습니다."),
                similarImages
        );
    }

    // --- [로직 1] 각 지표별 점수 계산 ---
    private List<PlantReportRes.MetricScore> calculateMetricScores(List<SensorComparisonRes> sensors) {
        List<PlantReportRes.MetricScore> scores = new ArrayList<>();

        for (SensorComparisonRes sensor : sensors) {
            double current = sensor.similarAvg() != null ? sensor.similarAvg() : 0;
            double ideal = sensor.betterAvg() != null ? sensor.betterAvg() : current; // 비교군 없으면 만점 처리

            // 점수 알고리즘: (1 - |현재-이상|/이상) * 100
            // 차이가 클수록 점수가 깎임. (최소 0점)
            double diffRatio = (ideal == 0) ? 0 : Math.abs(current - ideal) / ideal;
            int score = (int) Math.max(0, 100 - (diffRatio * 100 * 1.5)); // 1.5는 감점 가중치

            // 상태 판별
            String status = "적정";
            if (score < 80) {
                status = (current < ideal) ? "부족" : "과다";
            }

            scores.add(new PlantReportRes.MetricScore(sensor.sensorName(), score, status));
        }
        return scores;
    }

    // --- [로직 2] 전체 점수 계산 ---
    private int calculateOverallScore(String growthLevel, List<PlantReportRes.MetricScore> metricScores) {
        // 기본 점수 (성장 단계 기반)
        int baseScore = switch (growthLevel.toUpperCase()) {
            case "HIGH" -> 90;
            case "MEDIUM" -> 70;
            case "LOW" -> 50;
            default -> 30; // DIE
        };

        // 센서 점수 평균 반영 (가중치 30%)
        double avgMetricScore = metricScores.stream()
                .mapToInt(PlantReportRes.MetricScore::score)
                .average().orElse(0);

        return (int) (baseScore * 0.6 + avgMetricScore * 0.4);
    }

    // --- [로직 3] 문제되는 센서 Top 3 추출 ---
    private List<SensorComparisonRes> findTopIssues(List<SensorComparisonRes> sensors) {
        return sensors.stream()
                .filter(s -> s.betterAvg() != null && s.similarAvg() != null)
                .sorted((s1, s2) -> {
                    double diff1 = Math.abs(s1.similarAvg() - s1.betterAvg()) / s1.betterAvg();
                    double diff2 = Math.abs(s2.similarAvg() - s2.betterAvg()) / s2.betterAvg();
                    return Double.compare(diff2, diff1); // 내림차순 (차이 큰 순서)
                })
                .limit(3)
                .collect(Collectors.toList());
    }

    // --- [로직 4] Gemini API 호출 (조언 생성) ---
    private Map<String, String> askGeminiForAdvice(String plantName, String level, String caption,
                                                   List<SensorComparisonRes> allSensors,
                                                   List<SensorComparisonRes> topIssues) {
        // 프롬프트 구성
        String prompt = createSystemPrompt(plantName, level, caption, allSensors, topIssues);

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            String response = restClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Gemini Advice Error", e);
            return Map.of("analysis", "분석 중 오류 발생", "careGuide", "가이드 생성 실패");
        }
    }

    private String createSystemPrompt(String plantName, String level, String caption,
                                      List<SensorComparisonRes> allSensors,
                                      List<SensorComparisonRes> issues) {
        StringBuilder sensorInfo = new StringBuilder();
        for (SensorComparisonRes s : allSensors) {
            sensorInfo.append(String.format("- %s: Current %.1f (Ideal %.1f)\n",
                    s.sensorName(), s.similarAvg(), s.betterAvg()));
        }

        StringBuilder issueInfo = new StringBuilder();
        for (SensorComparisonRes s : issues) {
            double diff = s.similarAvg() - s.betterAvg();
            String status = diff > 0 ? "Higher" : "Lower";
            issueInfo.append(String.format("- %s is %s than ideal (%.1f vs %.1f)\n",
                    s.sensorName(), status, s.similarAvg(), s.betterAvg()));
        }

        return """
            You are a professional plant pathologist. Analyze the plant status and provide advice in Korean JSON format.
            
            [Plant Info]
            Name: %s, Health Level: %s
            Visual Symptoms: %s
            
            [Environmental Data Analysis]
            %s
            
            [Top 3 Major Issues]
            %s
            
            [Request]
            Return a JSON object with two fields:
            1. 'analysis': A comprehensive health analysis and environment-based advice. Mention the current weather or season if relevant to indoor gardening.
            2. 'careGuide': Detailed step-by-step care guide specifically addressing the 'Top 3 Major Issues'.
            
            Example Output Format:
            {
                "analysis": "현재 보스턴고사리는...",
                "careGuide": "1. 습도 관리: ..."
            }
            DO NOT use Markdown code blocks. Just raw JSON.
            """.formatted(plantName, level, caption, sensorInfo, issueInfo);
    }

    private Map<String, String> parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Markdown 제거
            text = text.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode json = objectMapper.readTree(text);
            return Map.of(
                    "analysis", json.path("analysis").asText(),
                    "careGuide", json.path("careGuide").asText()
            );
        } catch (Exception e) {
            log.error("JSON Parse Error", e);
            return Map.of();
        }
    }
}