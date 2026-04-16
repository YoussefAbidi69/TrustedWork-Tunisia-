package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;

/**
 * DTO de création d'un signalement de profil.
 */
@Data
public class AddReportRequest {

    @NotNull(message = "Le reporterId est obligatoire")
    private Long reporterId;

    @NotNull(message = "La catégorie est obligatoire")
    private ReportCategory category;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;
}