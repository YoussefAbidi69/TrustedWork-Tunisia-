package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.time.LocalDateTime;

/**
 * Signalement d'un profil frauduleux ou inapproprié
 * Workflow de modération admin
 */
@Entity
@Table(name = "profile_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID de l'utilisateur qui signale (référence Module 01)
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.PENDING;

    private LocalDateTime reportedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt; // Null tant que non traité

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;
}