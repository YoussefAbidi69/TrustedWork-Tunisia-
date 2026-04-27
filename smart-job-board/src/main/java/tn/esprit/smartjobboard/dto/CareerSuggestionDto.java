package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CareerSuggestionDto {
    private Long id;
    private String suggestedSkill;
    private Double trendScore;
    private Double coOccurrenceRate;
    private Double estimatedIncomeImpact;
    private String trend;
}

