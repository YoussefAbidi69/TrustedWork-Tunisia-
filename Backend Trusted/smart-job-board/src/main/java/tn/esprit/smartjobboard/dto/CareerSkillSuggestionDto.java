package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CareerSkillSuggestionDto {
    private String skill;
    private double combinedScore;
    private double trendComponent;
    private double coOccurrenceComponent;
    private int estimatedIncomeIncreasePercent;
}
