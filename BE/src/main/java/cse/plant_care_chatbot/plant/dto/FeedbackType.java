package cse.plant_care_chatbot.plant.dto;

public enum FeedbackType {
    IMPROVED("호전됨"),
    NO_CHANGE("변화 없음"),
    WORSENED("악화됨");

    private final String description;

    FeedbackType(String description) {
        this.description = description;
    }
}