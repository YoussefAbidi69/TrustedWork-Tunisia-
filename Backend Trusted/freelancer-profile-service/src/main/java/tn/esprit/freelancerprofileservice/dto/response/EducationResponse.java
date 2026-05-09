package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de réponse — parcours académique
 */
@Data
@Builder
public class EducationResponse {

    private Long id;
    private String degree;
    private String institution;
    private String fieldOfStudy;
    private Integer graduationYear;
}