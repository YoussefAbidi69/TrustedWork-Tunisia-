package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO d'ajout d'un parcours académique
 */
@Data
public class AddEducationRequest {

    @NotBlank(message = "Le diplôme est obligatoire")
    private String degree;

    @NotBlank(message = "L'établissement est obligatoire")
    private String institution;

    private String fieldOfStudy;
    private Integer graduationYear;
}