package cse.plant_care_chatbot.plant.dto;

public record GeminiAnalysisResult (
    String plantName,   // 추론된 식물 이름 (예: 보스턴고사리)
    String growthLevel, // 추론된 성장 단계 (High, Medium, Low, Die)
    String caption     // 이미지 캡션 (건강 상태 묘사)
){}
