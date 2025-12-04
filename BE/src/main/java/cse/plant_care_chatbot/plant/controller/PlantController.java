package cse.plant_care_chatbot.plant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cse.plant_care_chatbot.plant.dto.*;
import cse.plant_care_chatbot.plant.entity.PlantAnalysisLog;
import cse.plant_care_chatbot.plant.repository.PlantAnalysisLogRepository;
import cse.plant_care_chatbot.plant.service.GeminiEmbeddingService;
import cse.plant_care_chatbot.plant.service.MilvusService;
import cse.plant_care_chatbot.plant.service.PlantAnalysisService; // 새로 만든 서비스
import cse.plant_care_chatbot.plant.service.PlantService;
import cse.plant_care_chatbot.global.common.CommonResponse;
import cse.plant_care_chatbot.global.common.code.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/plants")
public class PlantController {

    private final PlantService plantService;           // Step 1: 이미지 분석
    private final PlantAnalysisLogRepository logRepo;  // 로그 저장
    private final GeminiEmbeddingService embedService; // 임베딩
    private final MilvusService milvusService;         // Step 2: 검색 & 센서 비교
    private final PlantAnalysisService reportService;  // Step 3: 최종 리포트 생성 (추가됨)

    private final ObjectMapper objectMapper;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<PlantReportRes> analyzePlant(
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "description", required = false) String description
    ) throws IOException {

        // 1️⃣ Step 1: Gemini 이미지 분석
        String safeDesc = (description != null) ? description : "";
        GeminiAnalysisResult geminiRes = plantService.analyzePlant(image, safeDesc);

        // 💾 DB 저장
        PlantAnalysisLog log = logRepo.save(PlantAnalysisLog.builder()
                .plantName(geminiRes.plantName())
                .growthLevel(geminiRes.growthLevel())
                .caption(geminiRes.caption())
                .userDescription(safeDesc)
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
                log.getId(),
                geminiRes.plantName(),
                geminiRes.growthLevel(),
                geminiRes.caption(),
                safeDesc,
                similarImages,
                sensorAnalysis
        );

        String resultJson = objectMapper.writeValueAsString(finalReport);
        log.updateResult(resultJson);
        logRepo.save(log);

        return CommonResponse.success(SuccessCode.PLANT_ANALYSIS_SUCCESS, finalReport);
    }

    @GetMapping("/history")
    public CommonResponse<List<HistoryListRes>> getHistory() {
        List<HistoryListRes> history = logRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(log -> new HistoryListRes(
                        log.getId(),
                        log.getPlantName(),
                        log.getGrowthLevel(),
                        log.getUserDescription(),
                        log.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return CommonResponse.success(SuccessCode.PLANT_ANALYSIS_SUCCESS, history);
    }

    @GetMapping("/history/{id}")
    public CommonResponse<PlantReportRes> getHistoryDetail(@PathVariable Long id) throws IOException {
        PlantAnalysisLog log = logRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 기록을 찾을 수 없습니다."));

        if (log.getAnalysisResult() == null) {
            throw new RuntimeException("분석 결과가 저장되지 않은 기록입니다.");
        }

        // JSON 문자열을 다시 객체(PlantReportRes)로 변환
        PlantReportRes result = objectMapper.readValue(log.getAnalysisResult(), PlantReportRes.class);

        return CommonResponse.success(SuccessCode.PLANT_ANALYSIS_SUCCESS, result);
    }

    @PatchMapping("/history/{logId}/feedback")
    public CommonResponse<Void> addFeedback(
            @PathVariable Long logId,
            @RequestBody PlantFeedbackReq feedbackReq
    ) {
        plantService.addFeedback(logId, feedbackReq);
        return CommonResponse.success(SuccessCode.PLANT_FEEDBACK_SUCCESS);
    }
}