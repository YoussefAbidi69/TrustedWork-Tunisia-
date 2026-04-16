package tn.esprit.freelancerprofileservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import tn.esprit.freelancerprofileservice.enums.CertificationType;

import java.time.LocalDate;

/**
 * DTO de mise à jour d'une certification
 * Tous les champs sont optionnels pour permettre une mise à jour flexible.
 */
@Data
public class UpdateCertificationRequest {

    @Size(min = 2, max = 120, message = "Le titre doit contenir entre 2 et 120 caractères")
    private String title;

    @Size(min = 2, max = 120, message = "L'émetteur doit contenir entre 2 et 120 caractères")
    private String issuer;

    private CertificationType type;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @URL(message = "L'URL du certificat est invalide")
    @Size(max = 500, message = "L'URL du certificat ne doit pas dépasser 500 caractères")
    private String certificateUrl;

    @AssertTrue(message = "La date d'émission doit être antérieure ou égale à la date d'expiration")
    public boolean isIssueDateBeforeOrEqualExpiryDate() {
        if (issueDate == null || expiryDate == null) {
            return true;
        }
        return !issueDate.isAfter(expiryDate);
    }
}