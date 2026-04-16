package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour l'ajout d'un avis client sur un profil freelancer
 *
 * Validations :
 * - rating entre 1 et 5
 * - commentaire obligatoire avec taille minimale
 */
@Data
public class AddReviewRequest {

    @NotNull(message = "Le clientId est obligatoire")
    private Long clientId;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Integer rating;

    @NotBlank(message = "Le commentaire est obligatoire")
    @Size(
            min = 20,
            max = 1000,
            message = "Le commentaire doit contenir entre 20 et 1000 caractères"
    )
    private String comment;
}