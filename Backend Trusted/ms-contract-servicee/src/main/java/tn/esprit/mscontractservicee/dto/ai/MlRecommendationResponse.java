package tn.esprit.mscontractservicee.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DTO used only for parsing the external FastAPI ML service response.
 * The ML service returns snake_case JSON properties.
 */
@Data
public class MlRecommendationResponse implements Serializable {
    @JsonProperty("freelancer_id")
    private String freelancerId;

    private String name;
    private String category;

    @JsonProperty("experience_level")
    private String experienceLevel;

    @JsonProperty("hourly_rate_usd")
    private Double hourlyRateUsd;

    @JsonProperty("avg_rating")
    private Double avgRating;

    @JsonProperty("success_proba")
    private Double successProba;

    @JsonProperty("semantic_score")
    private Double semanticScore;

    @JsonProperty("final_score")
    private Double finalScore;

    private String cin;
    private List<String> skills;

    public RecommendationResponse toPublicDto() {
        RecommendationResponse dto = new RecommendationResponse();
        dto.setFreelancerId(freelancerId);
        dto.setName(name);
        dto.setCategory(category);
        dto.setExperienceLevel(experienceLevel);
        dto.setHourlyRateUsd(hourlyRateUsd);
        dto.setAvgRating(avgRating);
        dto.setSuccessProba(successProba);
        dto.setSemanticScore(semanticScore);
        dto.setFinalScore(finalScore);
        dto.setCin(cin);
        dto.setSkills(skills);
        return dto;
    }
}

