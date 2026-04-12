package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de signalement d'un profil
 */
@Data
public class AddReportRequest {

    @NotNull(message = "Le reporterId est obligatoire")
    private Long reporterId;

    @NotBlank(message = "La raison est obligatoire")
    private String reason;
}