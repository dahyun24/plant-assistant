package cse.plant_care_chatbot.plant.controller;

import cse.plant_care_chatbot.global.common.CommonResponse;
import cse.plant_care_chatbot.plant.dto.GeminiAnalysisResult;
import cse.plant_care_chatbot.plant.service.PlantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static cse.plant_care_chatbot.global.common.code.SuccessCode.PLANT_ANALYSIS_SUCCESS;

@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    @PostMapping("/analyze")
    public CommonResponse<GeminiAnalysisResult> analyzePlant(
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "description", required = false) String description
    ) {
        String safeDescription = (description != null) ? description : "";
        try {
            GeminiAnalysisResult result = plantService.analyzePlant(image, safeDescription);
            return CommonResponse.success(PLANT_ANALYSIS_SUCCESS, result);
        } catch (IOException e) {
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }
    }
}