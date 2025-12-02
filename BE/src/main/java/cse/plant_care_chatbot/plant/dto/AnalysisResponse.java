package cse.plant_care_chatbot.plant.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AnalysisResponse {

    // [Section 1] 전체 건강도 (예: 85)
    private int totalHealthScore;

    // [Section 2] 상태 지표 리스트 (온도, 습도 등 각각의 점수)
    private List<SensorMetric> sensorMetrics;

    // [Section 3] 발견된 문제 및 종합 분석 (Gemini 생성 텍스트)
    private String foundProblems;      // "발견된 문제" 섹션
    private String comprehensiveAdvice; // 종합 조언
    private String detailedGuide;      // 세부 관리 가이드

    // [Section 4] 유사 이미지
    private List<String> similarImageUrls;

    @Data
    @Builder
    public static class SensorMetric {
        private String label;    // 화면 표시 이름 (예: "온도", "습도")
        private int score;       // 0~100 점수
        private String status;   // "적정", "주의", "위험" (색상 결정용)
    }
}