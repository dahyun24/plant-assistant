package cse.plant_care_chatbot.plant.repository;

import cse.plant_care_chatbot.plant.entity.PlantAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantAnalysisLogRepository extends JpaRepository<PlantAnalysisLog, Long> {
}
