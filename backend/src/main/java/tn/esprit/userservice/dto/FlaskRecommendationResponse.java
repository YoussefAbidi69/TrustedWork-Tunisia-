package tn.esprit.userservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class FlaskRecommendationResponse {
    private Long agency_id;
    private List<FlaskRecommendationDTO> recommendations;

    @Data
    public static class FlaskRecommendationDTO {
        private Long freelancer_id;
        private float recommendation_score;
        private float skill_match_score;
        private float trust_score;
        private float experience_score;
        private float availability_score;
        private float similarity_score;
        private float location_score;
        private String explanation;
    }
}
