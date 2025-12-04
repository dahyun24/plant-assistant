package cse.plant_care_chatbot.plant.entity;

import cse.plant_care_chatbot.plant.dto.FeedbackType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PlantAnalysisLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String plantName;

    private String growthLevel;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String userDescription;

    @Column(columnDefinition = "LONGTEXT")
    private String analysisResult;

    private String originalImageUrl;

    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;

    @Column(columnDefinition = "TEXT")
    private String feedbackComment;

    private LocalDateTime feedbackDate;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public PlantAnalysisLog(String plantName, String growthLevel, String caption, String userDescription, String analysisResult, String originalImageUrl) {
        this.plantName = plantName;
        this.growthLevel = growthLevel;
        this.caption = caption;
        this.userDescription = userDescription;
        this.analysisResult = analysisResult;
        this.originalImageUrl = originalImageUrl;
    }

    public void updateResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }

    public void updateFeedback(FeedbackType feedbackType, String feedbackComment) {
        this.feedbackType = feedbackType;
        this.feedbackComment = feedbackComment;
        this.feedbackDate = LocalDateTime.now();
    }
}