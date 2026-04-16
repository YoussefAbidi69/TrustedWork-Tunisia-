package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de mise à jour d'un parcours académique
 */
@Data
public class UpdateEducationRequest {

    @Size(min = 2, max = 100, message = "Le diplôme doit contenir entre 2 et 100 caractères")
    private String degree;

    @Size(min = 2, max = 100, message = "L'établissement doit contenir entre 2 et 100 caractères")
    private String institution;

    @Size(max = 100, message = "Le domaine d'études ne doit pas dépasser 100 caractères")
    private String fieldOfStudy;

    @Min(value = 1950, message = "L'année d'obtention doit être supérieure à 1950")
    @Max(value = 2100, message = "L'année d'obtention ne peut pas être dans le futur")
    private Integer graduationYear;
}