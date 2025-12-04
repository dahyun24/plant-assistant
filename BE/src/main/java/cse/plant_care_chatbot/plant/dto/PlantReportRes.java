package cse.plant_care_chatbot.plant.dto;

import java.util.List;

public record PlantReportRes(
        Long logId,
        String plantName,
        // 1. 전체 건강도 점수 (0~100)
        int overallScore,

        // 2. 각 지표별 점수 (잘 자란 식물 대비)
        List<MetricScore> metricScores,

        // 3. 이미지 캡션 (Step 1 결과)
        String caption,

        // 4. 종합 분석 및 환경 맞춤 조언 (LLM 생성)
        String analysis,

        // 핵심 키워드 (예: ["물 부족", "강한 빛", "통풍 필요"])
        List<String> keywords,

        // 5. 세부 관리 가이드 (Top 3 문제점, LLM 생성)
        List<CareGuide> careGuide,

        // 6. 유사한 식물 3가지 (Step 2 결과)
        List<String> similarImages
) {
    public record MetricScore(
            String sensorName,
            int score,
            String status
    ) {}

    public record CareGuide(
            String issue,
            String content
    ) {}
}