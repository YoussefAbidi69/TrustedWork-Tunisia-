package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerInsightResponse {
    private String targetRole;
    private String currentLevel;
    private Integer totalWeeks;
    private Double totalIncomeBoost;
    private Double currentRate;
    private Double projectedRate;
    private String difficulty;
    private List<CareerRoadmapStepDto> steps;
}
