package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreBreakdownDto {
    private double skillMatch;
    private double reputation;
    private double successRate;
    private double budgetFit;
    private double availability;
    private double totalScore;
}
