package cse.plant_care_chatbot.plant.entity;

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
}