package cse.plant_care_chatbot.plant.controller;

import cse.plant_care_chatbot.plant.dto.GeminiAnalysisResult;
import cse.plant_care_chatbot.plant.dto.PlantReportRes;
import cse.plant_care_chatbot.plant.dto.SensorComparisonRes;
import cse.plant_care_chatbot.plant.entity.PlantAnalysisLog;
import cse.plant_care_chatbot.plant.repository.PlantAnalysisLogRepository;
import cse.plant_care_chatbot.plant.service.GeminiEmbeddingService;
import cse.plant_care_chatbot.plant.service.MilvusService;
import cse.plant_care_chatbot.plant.service.PlantAnalysisService; // 새로 만든 서비스
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

    private final PlantService plantService;           // Step 1: 이미지 분석
    private final PlantAnalysisLogRepository logRepo;  // 로그 저장
    private final GeminiEmbeddingService embedService; // 임베딩
    private final MilvusService milvusService;         // Step 2: 검색 & 센서 비교
    private final PlantAnalysisService reportService;  // Step 3: 최종 리포트 생성 (추가됨)

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<PlantReportRes> analyzePlant(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "description", required = false) String description
    ) throws IOException {

        // 1️⃣ Step 1: Gemini 이미지 분석
        String safeDesc = (description != null) ? description : "";
        GeminiAnalysisResult geminiRes = plantService.analyzePlant(image, safeDesc);

        // 💾 DB 저장
        logRepo.save(PlantAnalysisLog.builder()
                .plantName(geminiRes.plantName())
                .growthLevel(geminiRes.growthLevel())
                .caption(geminiRes.caption())
                .build());

        // 2️⃣ Step 2: Milvus 검색 및 센서 데이터 비교
        List<Float> captionVector = embedService.getEmbedding(geminiRes.caption());

        Map<String, Object> milvusResult = milvusService.searchAndAnalyze(
                geminiRes.plantName(),
                geminiRes.growthLevel(),
                captionVector
        );

        List<String> similarImages = (List<String>) milvusResult.get("images");
        List<SensorComparisonRes> sensorAnalysis = (List<SensorComparisonRes>) milvusResult.get("analysis");

        // 3️⃣ Step 3: 최종 리포트 생성 (점수 계산 + 상세 가이드)
        PlantReportRes finalReport = reportService.generateReport(
                geminiRes.plantName(),
                geminiRes.growthLevel(),
                geminiRes.caption(),
                similarImages,
                sensorAnalysis
        );

        return CommonResponse.success(SuccessCode.PLANT_ANALYSIS_SUCCESS, finalReport);
    }
}