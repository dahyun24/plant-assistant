package cse.plant_care_chatbot.plant.service;

import cse.plant_care_chatbot.plant.dto.GeminiAnalysisResult;
import org.springframework.web.multipart.MultipartFile;

public interface GeminiService {
    /**
     * 이미지와 성장 단계를 받아 캡션(설명) 생성
     */
    GeminiAnalysisResult analyzeImage(MultipartFile image);

    /**
     * 프롬프트를 받아 일반 텍스트 응답 생성
     */
    String generateContent(String prompt);
}
