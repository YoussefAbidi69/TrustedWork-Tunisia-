package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO d'ajout d'un avis client
 */
@Data
public class AddReviewRequest {

    @NotNull(message = "Le clientId est obligatoire")
    private Long clientId;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Integer rating;

    private String comment;
}