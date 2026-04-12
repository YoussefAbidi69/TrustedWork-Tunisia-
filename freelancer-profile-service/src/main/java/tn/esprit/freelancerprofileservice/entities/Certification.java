package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.CertificationType;

import java.time.LocalDate;

/**
 * Certification obtenue par le freelancer
 * Vérifiée par le scheduler mensuel (expiry check)
 */
@Entity
@Table(name = "certifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // Ex: "AWS Solutions Architect"

    private String issuer; // Ex: "Amazon Web Services"

    @Enumerated(EnumType.STRING)
    private CertificationType type;

    private LocalDate issueDate;
    private LocalDate expiryDate; // Null = pas d'expiration

    private String certificateUrl; // Lien de vérification

    private Boolean isExpired = false; // Mis à jour par le scheduler

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;
}