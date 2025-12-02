package cse.plant_care_chatbot.plant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cse.plant_care_chatbot.plant.dto.GeminiAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper;

    public GeminiAnalysisResult analyzePlant(MultipartFile image, String description) throws IOException {
        String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType(); // e.g., "image/jpeg"

        // Gemini 요청 본문 구성
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", getSystemPrompt() + "\nUser Description: " + description),
                                        Map.of("inline_data", Map.of(
                                                "mime_type", mimeType != null ? mimeType : "image/jpeg",
                                                "data", base64Image
                                        ))
                                )
                        )
                )
        );

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        String response = restClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseGeminiResponse(response);
    }

    private String getSystemPrompt() {
        return """
                Analyze the plant in this image and extract the following information in JSON format:
                1. 'plantName': Identify the specific plant name in Korean. **You MUST choose ONLY between '보스턴고사리' and '스파티필럼'.** Do not output any other plant name.                
                2. 'growthLevel': Estimate the growth stage as one of ['High', 'Medium', 'Low', 'DIE'].
                   - 'High': Healthy, lush, vibrant green.
                   - 'Medium': Average condition, minor issues.
                   - 'Low': Visible wilting, discoloration, poor health.
                   - 'DIE': Dead or dying.
                3. 'caption': A detailed description of the plant's visual health status in Korean. Focus on leaf color, drooping, and vitality.
                
                Output ONLY the JSON object. Do not include markdown code blocks.
                """;
    }

    private GeminiAnalysisResult parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            // Gemini 응답 구조: candidates[0].content.parts[0].text
            String jsonText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 마크다운 코드 블록 제거 (```json ... ```)
            jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

            return objectMapper.readValue(jsonText, GeminiAnalysisResult.class);
        } catch (Exception e) {
            log.error("Gemini Response Parsing Error: ", e);
            throw new RuntimeException("식물 분석 중 오류가 발생했습니다.");
        }
    }
}