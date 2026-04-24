package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO d'ajout d'un parcours académique
 * Validations :
 * - degree et institution obligatoires
 * - graduationYear entre 1950 et l'année courante (pas dans le futur)
 */
@Data
public class AddEducationRequest {

    @NotBlank(message = "Le diplôme est obligatoire")
    @Size(min = 2, max = 100, message = "Le diplôme doit contenir entre 2 et 100 caractères")
    private String degree;

    @NotBlank(message = "L'établissement est obligatoire")
    @Size(min = 2, max = 100, message = "L'établissement doit contenir entre 2 et 100 caractères")
    private String institution;

    @Size(max = 100, message = "Le domaine d'études ne doit pas dépasser 100 caractères")
    private String fieldOfStudy;

    @Min(value = 1950, message = "L'année d'obtention doit être supérieure à 1950")
    @Max(value = 2100, message = "L'année d'obtention ne peut pas être dans le futur")
    private Integer graduationYear;
}