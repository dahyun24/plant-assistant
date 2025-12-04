package cse.plant_care_chatbot.plant.dto;

import java.time.LocalDateTime;

public record HistoryListRes(
        Long id,
        String plantName,
        String growthLevel,
        String userDescription,
        LocalDateTime createdAt
) {}