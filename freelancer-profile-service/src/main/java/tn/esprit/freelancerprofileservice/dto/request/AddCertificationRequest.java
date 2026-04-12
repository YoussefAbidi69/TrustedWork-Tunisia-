package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.CertificationType;

import java.time.LocalDate;

/**
 * DTO d'ajout d'une certification
 */
@Data
public class AddCertificationRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    private String issuer;
    private CertificationType type;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String certificateUrl;
}