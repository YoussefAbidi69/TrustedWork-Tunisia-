package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;

/**
 * DTO de réponse — compétence avec score d'authenticité
 */
@Data
@Builder
public class SkillResponse {

    private Long id;
    private String name;
    private SkillLevel level;
    private Double authenticityScore;
    private Double examScore;
    private long endorsementCount;
}