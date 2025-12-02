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

    private String growthLevel; // High, Medium, Low, Die

    @Column(columnDefinition = "TEXT")
    private String caption;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public PlantAnalysisLog(String plantName, String growthLevel, String caption) {
        this.plantName = plantName;
        this.growthLevel = growthLevel;
        this.caption = caption;
    }
}