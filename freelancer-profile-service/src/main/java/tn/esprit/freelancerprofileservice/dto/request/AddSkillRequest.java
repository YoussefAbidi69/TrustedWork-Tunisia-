package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO d'ajout d'une compétence
 */
@Data
public class AddSkillRequest {

    @NotBlank(message = "Le nom du skill est obligatoire")
    private String name;

    private Double examScore = 0.0;
}