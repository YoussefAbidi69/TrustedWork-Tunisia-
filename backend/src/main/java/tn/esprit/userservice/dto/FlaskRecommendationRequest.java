package tn.esprit.userservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FlaskRecommendationRequest {
    private Long agency_id;
    private List<FlaskCandidateDTO> candidates;

    @Data
    @Builder
    public static class FlaskCandidateDTO {
        private Long freelancer_id;
        private float skill_match_score;
        private float trust_score;
        private float experience_score;
        private float availability_score;
        private float similarity_score;
        private float location_score;
        private float kyc_bonus;
        private float liveness_bonus;
    }
}
