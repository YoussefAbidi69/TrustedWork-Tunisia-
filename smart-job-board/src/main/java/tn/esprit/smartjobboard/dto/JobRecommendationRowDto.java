package tn.esprit.smartjobboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobRecommendationRowDto {
    private Long jobOfferId;
    private String title;
    private String category;
    private double matchScore;
    private double opportunityScore;
    private double freshnessFactor;
    private double rankingScore;
    /** Alias of freshnessFactor for API consumers */
    private double freshnessScore;
    /** Alias of rankingScore for API consumers */
    private double recommendationScore;
    private double successProbability;
    private String confidence;
    private JobOfferResponse job;
    private java.util.List<String> topMatchingSkills;
}
