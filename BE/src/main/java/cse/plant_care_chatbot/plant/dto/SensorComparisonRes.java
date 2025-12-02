package cse.plant_care_chatbot.plant.dto;

public record SensorComparisonRes(
        String sensorName,
        Double similarAvg,  // 유사 상태 평균
        Double betterAvg,   // 더 잘 자란 상태 평균
        Double worseAvg    // 더 못 자란 상태 평균
) {
}