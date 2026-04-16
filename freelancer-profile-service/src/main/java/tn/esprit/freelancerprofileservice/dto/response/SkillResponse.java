package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.SkillCategory;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;

/**
 * DTO de réponse d'une compétence.
 */
@Data
@Builder
public class SkillResponse {

    private Long id;
    private String name;
    private SkillCategory category;
    private SkillLevel level;
    private Double authenticityScore;
    private Double examScore;
    private Integer endorsementCount;
}