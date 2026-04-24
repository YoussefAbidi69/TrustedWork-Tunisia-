package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO d'ajout d'un endorsement sur un skill.
 */
@Data
public class AddEndorsementRequest {

    @NotNull(message = "L'endorserId est obligatoire")
    private Long endorserId;

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    private String comment;
}