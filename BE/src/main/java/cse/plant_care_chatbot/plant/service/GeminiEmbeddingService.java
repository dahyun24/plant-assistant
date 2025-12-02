package cse.plant_care_chatbot.plant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiEmbeddingService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final String EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";

    public List<Float> getEmbedding(String text) {
        RestClient restClient = RestClient.create();

        // 요청 바디 생성
        Map<String, Object> requestBody = Map.of(
                "model", "models/gemini-embedding-001",
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                )
        );

        try {
            String response = restClient.post()
                    .uri(EMBEDDING_URL + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseEmbeddingResponse(response);
        } catch (Exception e) {
            log.error("Embedding API Error", e);
            throw new RuntimeException("임베딩 생성 실패");
        }
    }

    private List<Float> parseEmbeddingResponse(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);
            JsonNode values = root.path("embedding").path("values");

            List<Float> vector = new ArrayList<>();
            if (values.isArray()) {
                for (JsonNode val : values) {
                    vector.add((float) val.asDouble());
                }
            }
            return vector;
        } catch (Exception e) {
            throw new RuntimeException("임베딩 응답 파싱 실패", e);
        }
    }
}