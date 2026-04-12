package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;

import java.time.LocalDateTime;

/**
 * Avis d'un client sur le travail d'un freelancer
 * Anti-spam : 1 review max par client par freelancer (contrainte DB)
 */
@Entity
@Table(
        name = "profile_reviews",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"client_id", "profile_id"},
                name = "uk_client_profile_review"
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID du client qui laisse l'avis (référence Module 01)
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Integer rating; // Note de 1 à 5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.VISIBLE;

    private LocalDateTime reviewedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;
}