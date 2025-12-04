package cse.plant_care_chatbot.plant.dto;

public record PlantFeedbackReq(
        FeedbackType feedbackType,
        String comment
) {}