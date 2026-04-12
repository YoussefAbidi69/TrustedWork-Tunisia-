package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO d'ajout d'un endorsement sur un skill
 */
@Data
public class AddEndorsementRequest {

    @NotNull(message = "L'endorserId est obligatoire")
    private Long endorserId;

    private String comment;
}