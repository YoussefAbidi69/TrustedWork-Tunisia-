package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.freelancerprofileservice.enums.CertificationType;

import java.time.LocalDate;

/**
 * DTO de réponse — certification
 */
@Data
@Builder
public class CertificationResponse {

    private Long id;
    private String title;
    private String issuer;
    private CertificationType type;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String certificateUrl;
    private Boolean isExpired;
}