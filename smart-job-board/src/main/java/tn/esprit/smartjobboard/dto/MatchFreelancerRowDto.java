package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchFreelancerRowDto {
    private Long freelancerId;
    private String email;
    private double totalMatchScore;
    private double skillMatch;
    private double reputation;
    private double successRate;
    private double budgetFit;
    private double availability;
    private double successProbability;
    private String predictionConfidence;
}
