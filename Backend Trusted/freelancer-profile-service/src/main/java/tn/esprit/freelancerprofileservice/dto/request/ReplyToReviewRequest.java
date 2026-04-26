package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO pour permettre au freelancer de répondre à un avis
 */
@Data
public class ReplyToReviewRequest {

    @NotBlank(message = "La réponse ne peut pas être vide")
    @Size(
            min = 5,
            max = 1000,
            message = "La réponse doit contenir entre 5 et 1000 caractères"
    )
    private String reply;
}