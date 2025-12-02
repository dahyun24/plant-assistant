package cse.plant_care_chatbot.plant.dto;

import java.util.List;

public record PlantResultRes(
        // Step 1: Gemini 분석 결과
        Long logId,
        String plantName,
        String growthLevel,
        String caption,

        // Step 2: Milvus 검색 결과
        List<String> similarImages,              // 유사 식물 이미지 3개
        List<SensorComparisonRes> sensorAnalysis // 센서 비교 테이블 데이터
) {
}