package cse.plant_care_chatbot.plant.service.impl;

import cse.plant_care_chatbot.plant.dto.GeminiAnalysisResult;
import cse.plant_care_chatbot.plant.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public GeminiAnalysisResult analyzeImage(MultipartFile image) {
        // 1. 추론을 위한 강력한 프롬프트 작성
        String prompt = """
            Analyze the plant in this image and extract the following information in JSON format:
            1. 'plantName': Identify the specific plant name in Korean (e.g., 보스턴고사리, 스파티필럼, 스투키).
            2. 'growthLevel': Estimate the growth stage as one of ['High', 'Medium', 'Low', 'Die'].
               - 'High': Healthy, lush, vibrant green.
               - 'Medium': Average condition, minor issues.
               - 'Low': Visible wilting, discoloration, poor health.
               - 'DIE': Dead or dying.
            3. 'caption': A detailed description of the plant's visual health status in Korean. Focus on leaf color, drooping, and vitality.
            
            Output ONLY the JSON object. Do not include markdown code blocks.
            """;

        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            // 2. Gemini API 호출
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt),
                                    Map.of("inline_data", Map.of(
                                            "mime_type", mimeType,
                                            "data", base64Image
                                    ))
                            ))
                    ),
                    // JSON 응답 강제 (Gemini 1.5 기능 활용)
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json"
                    )
            );

            String response = callGeminiApi(requestBody);

            // 3. JSON 파싱 -> DTO 변환
            return objectMapper.readValue(response, GeminiAnalysisResult.class);

        } catch (IOException e) {
            log.error("Image analysis failed", e);
            throw new RuntimeException("이미지 분석 중 오류가 발생했습니다.");
        }
    }


    @Override
    public String generateContent(String prompt) {
        // 텍스트 전용 요청 바디 생성
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
        return callGeminiApi(requestBody);
    }

    private String callGeminiApi(Map<String, Object> requestBody) {
        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path(GEMINI_API_URL) // 절대 경로인 경우 uri(String) 사용 권장, 여기선 예시
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 응답 파싱 (candidates[0].content.parts[0].text)
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            return "AI 분석을 완료할 수 없습니다."; // Fallback 메시지
        }
    }
}
