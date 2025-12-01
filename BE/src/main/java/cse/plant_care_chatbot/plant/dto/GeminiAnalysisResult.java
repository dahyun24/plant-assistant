package cse.plant_care_chatbot.plant.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiAnalysisResult {
    private String plantName;   // 추론된 식물 이름 (예: 보스턴고사리)
    private String growthLevel; // 추론된 성장 단계 (High, Medium, Low, Die)
    private String caption;     // 이미지 캡션 (건강 상태 묘사)
}
