package cse.plant_care_chatbot.plant.controller;

import cse.plant_care_chatbot.plant.dto.GeminiAnalysisResult;
import cse.plant_care_chatbot.plant.dto.PlantResultRes;
import cse.plant_care_chatbot.plant.dto.SensorComparisonRes;
import cse.plant_care_chatbot.plant.entity.PlantAnalysisLog;
import cse.plant_care_chatbot.plant.repository.PlantAnalysisLogRepository;
import cse.plant_care_chatbot.plant.service.GeminiEmbeddingService;
import cse.plant_care_chatbot.plant.service.MilvusService;
import cse.plant_care_chatbot.plant.service.PlantService;
import cse.plant_care_chatbot.global.common.CommonResponse;
import cse.plant_care_chatbot.global.common.code.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/plants")
public class PlantController {

    private final PlantService plantService;          // Step 1: Gemini Vision
    private final PlantAnalysisLogRepository logRepo; // Save: DB 저장
    private final GeminiEmbeddingService embedService;// Step 2-1: 캡션 임베딩
    private final MilvusService milvusService;        // Step 2-2: 검색 & 비교

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<PlantResultRes> analyzePlant(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "description", required = false) String description
    ) throws IOException {

        // -------------------------------------------------------
        // 1️⃣ Step 1: Gemini 이미지 분석 (식물명, 상태, 캡션)
        // -------------------------------------------------------
        String safeDesc = (description != null) ? description : "";
        GeminiAnalysisResult geminiRes = plantService.analyzePlant(image, safeDesc);

        // -------------------------------------------------------
        // 💾 Save: 분석 결과 DB 저장 (Log)
        // -------------------------------------------------------
        PlantAnalysisLog savedLog = logRepo.save(PlantAnalysisLog.builder()
                .plantName(geminiRes.plantName())
                .growthLevel(geminiRes.growthLevel())
                .caption(geminiRes.caption())
                .build());

        // -------------------------------------------------------
        // 2️⃣ Step 2: 벡터 검색 및 센서 비교
        // -------------------------------------------------------
        // (1) 캡션을 벡터로 변환 (Gemini Embedding-001)
        List<Float> captionVector = embedService.getEmbedding(geminiRes.caption());

        // (2) Milvus 검색 수행
        Map<String, Object> milvusResult = milvusService.searchAndAnalyze(
                geminiRes.plantName(),
                geminiRes.growthLevel(),
                captionVector
        );

        // -------------------------------------------------------
        // 🚀 최종 반환 DTO 생성
        // -------------------------------------------------------
        PlantResultRes finalResponse = new PlantResultRes(
                savedLog.getId(),
                geminiRes.plantName(),
                geminiRes.growthLevel(),
                geminiRes.caption(),
                (List<String>) milvusResult.get("images"),
                (List<SensorComparisonRes>) milvusResult.get("analysis")
        );

        return CommonResponse.success(SuccessCode.PLANT_ANALYSIS_SUCCESS, finalResponse);
    }
}