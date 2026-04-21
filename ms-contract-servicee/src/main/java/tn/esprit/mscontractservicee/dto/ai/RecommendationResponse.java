package tn.esprit.mscontractservicee.dto.ai;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class RecommendationResponse implements Serializable {
    private String freelancerId;
    private String name;
    private String category;
    private String experienceLevel;
    private Double hourlyRateUsd;
    private Double avgRating;
    private Double successProba;
    private Double semanticScore;
    private Double finalScore;
    private String cin;
    private List<String> skills;
}