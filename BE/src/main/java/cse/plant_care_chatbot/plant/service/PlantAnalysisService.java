package cse.plant_care_chatbot.plant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cse.plant_care_chatbot.plant.dto.PlantReportRes;
import cse.plant_care_chatbot.plant.dto.SensorComparisonRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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

    public PlantReportRes generateReport(String plantName, String growthLevel, String caption, String userDescription,
                                         List<String> similarImages, List<SensorComparisonRes> sensorData) {

        // 1. 점수 계산
        List<PlantReportRes.MetricScore> metricScores = calculateMetricScores(sensorData);

        // 2. 전체 점수 계산
        int overallScore = calculateOverallScore(growthLevel, metricScores);

        // 3. 편차가 큰 Top 3 센서 찾기
        List<SensorComparisonRes> topIssues = findTopIssues(sensorData);

        // 4. Gemini에게 조언 요청 (구조화된 응답 요청)
        GeminiResponse llmResponse = askGeminiForAdvice(plantName, growthLevel, caption, userDescription, sensorData, topIssues);

        return new PlantReportRes(
                plantName,
                overallScore,
                metricScores,
                caption,
                llmResponse.analysis(),
                llmResponse.keywords(),
                llmResponse.careGuide(),
                similarImages
        );
    }

    // ... (calculateMetricScores, calculateOverallScore, findTopIssues는 기존과 동일하므로 생략) ...
    // 아래 코드를 복사해서 덮어쓰세요.

    // --- [로직 4] Gemini API 호출 ---
    private GeminiResponse askGeminiForAdvice(String plantName, String level, String caption, String userDescription,
                                              List<SensorComparisonRes> allSensors,
                                              List<SensorComparisonRes> topIssues) {
        String prompt = createSystemPrompt(plantName, level, caption, userDescription, allSensors, topIssues);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        try {
            String response = restClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Gemini Advice Error : {}", e.getMessage());
            // 에러 시 기본값 반환
            return new GeminiResponse(
                    "AI 분석 서버 연결 지연으로 분석 정보를 불러오지 못했습니다.",
                    List.of("분석 실패"),
                    List.of(new PlantReportRes.CareGuide("안내", "잠시 후 다시 시도해주세요."))
            );
        }
    }

    private String createSystemPrompt(String plantName, String level, String caption, String userDescription,
                                      List<SensorComparisonRes> allSensors,
                                      List<SensorComparisonRes> issues) {
        StringBuilder sensorInfo = new StringBuilder();
        for (SensorComparisonRes s : allSensors) {
            double sim = s.similarAvg() != null ? s.similarAvg() : 0;
            double better = s.betterAvg() != null ? s.betterAvg() : 0;
            sensorInfo.append(String.format("- %s: Current %.1f (Ideal %.1f)\n", s.sensorName(), sim, better));
        }

        StringBuilder issueInfo = new StringBuilder();
        for (SensorComparisonRes s : issues) {
            double sim = s.similarAvg() != null ? s.similarAvg() : 0;
            double better = s.betterAvg() != null ? s.betterAvg() : 0;
            double diff = sim - better;
            String status = diff > 0 ? "Higher" : "Lower";
            issueInfo.append(String.format("- %s is %s than ideal (%.1f vs %.1f)\n", s.sensorName(), status, sim, better));
        }

        return """
            You are a professional plant pathologist. Analyze the plant status and provide advice in Korean JSON format.

            [Plant Info]
            Name: %s, Health Level: %s
            Visual Symptoms: %s

            [User Query/Description]
            %s
            (Please consider the user's specific situation or question in your analysis if provided.)
            
            [Environmental Data Analysis]
            %s

            [Top 3 Major Issues]
            %s

            [Request]
            Return a JSON object with the following fields:
            1. 'analysis': A comprehensive health analysis (Max 5 sentences). **DO NOT mention specific numbers.** Use qualitative terms like 'high', 'low', 'sufficient'. Mention the season if relevant.
            2. 'keywords': Extract 3-5 short keywords summarizing the status (e.g., "Water Shortage", "Too much light").
            3. 'careGuide': A list of guides for the 'Top 3 Major Issues'. Each guide must have an 'issue' (title) and 'content' (Max 3 sentences).

            Example Output Format:
            {
                "analysis": "현재 식물은 수분이 부족하여 잎이 마르고 있습니다. 빛이 너무 강해 잎 끝이 타는 증상도 보입니다...",
                "keywords": ["수분 부족", "강한 빛", "통풍 필요"],
                "careGuide": [
                    {
                        "issue": "수분 관리",
                        "content": "겉흙이 말랐을 때 물을 흠뻑 주세요. 저면관수 방식도 추천합니다."
                    },
                    {
                        "issue": "광량 조절",
                        "content": "직사광선을 피하고 반음지로 옮겨주세요."
                    }
                ]
            }
            Output ONLY the JSON object.
            """.formatted(plantName, level, caption, userDescription, sensorInfo, issueInfo);
    }

    private GeminiResponse parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            text = text.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode json = objectMapper.readTree(text);

            String analysis = json.path("analysis").asText();

            // 키워드 파싱
            List<String> keywords = objectMapper.convertValue(
                    json.path("keywords"), new TypeReference<List<String>>() {});

            // 케어 가이드 파싱
            List<PlantReportRes.CareGuide> careGuide = objectMapper.convertValue(
                    json.path("careGuide"), new TypeReference<List<PlantReportRes.CareGuide>>() {});

            return new GeminiResponse(analysis, keywords, careGuide);
        } catch (Exception e) {
            log.error("JSON Parse Error", e);
            return new GeminiResponse("분석 결과를 처리하는 중 오류가 발생했습니다.", List.of(), List.of());
        }
    }

    // 내부 사용용 레코드
    private record GeminiResponse(
            String analysis,
            List<String> keywords,
            List<PlantReportRes.CareGuide> careGuide
    ) {}

    // ... (이전과 동일한 메서드들: calculateMetricScores, calculateOverallScore, findTopIssues) ...
    private List<PlantReportRes.MetricScore> calculateMetricScores(List<SensorComparisonRes> sensors) {
        List<PlantReportRes.MetricScore> scores = new ArrayList<>();
        for (SensorComparisonRes sensor : sensors) {
            double current = sensor.similarAvg() != null ? sensor.similarAvg() : 0;
            double ideal = sensor.betterAvg() != null ? sensor.betterAvg() : current;
            double diffRatio = (ideal == 0) ? 0 : Math.abs(current - ideal) / ideal;
            int score = (int) Math.max(0, 100 - (diffRatio * 100 * 1.5));
            String status = "적정";
            if (score < 80) status = (current < ideal) ? "부족" : "과다";
            scores.add(new PlantReportRes.MetricScore(sensor.sensorName(), score, status));
        }
        return scores;
    }

    private int calculateOverallScore(String growthLevel, List<PlantReportRes.MetricScore> metricScores) {
        int baseScore = switch (growthLevel.toUpperCase()) {
            case "HIGH" -> 90;
            case "MEDIUM" -> 70;
            case "LOW" -> 50;
            default -> 30;
        };
        double avgMetricScore = metricScores.stream().mapToInt(PlantReportRes.MetricScore::score).average().orElse(0);
        return (int) (baseScore * 0.7 + avgMetricScore * 0.3);
    }

    private List<SensorComparisonRes> findTopIssues(List<SensorComparisonRes> sensors) {
        return sensors.stream()
                .filter(s -> s.betterAvg() != null && s.similarAvg() != null)
                .sorted((s1, s2) -> {
                    double diff1 = Math.abs(s1.similarAvg() - s1.betterAvg()) / s1.betterAvg();
                    double diff2 = Math.abs(s2.similarAvg() - s2.betterAvg()) / s2.betterAvg();
                    return Double.compare(diff2, diff1);
                })
                .limit(3)
                .collect(Collectors.toList());
    }
}