package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;

import java.time.LocalDateTime;

/**
 * Avis laissé sur un profil freelancer.
 *
 * Règles métier principales :
 * - Un même client ne peut laisser qu'un seul avis par profil
 * - Le freelancer peut répondre à l'avis
 * - Un avis peut être flaggé comme suspect par un algorithme métier
 */
@Entity
@Table(
        name = "profile_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"client_id", "profile_id"},
                        name = "uk_client_profile_review"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "client_id", nullable = false)
    private Long clientId;


    @Column(nullable = false)
    private Integer rating;


    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;


    @Column(name = "freelancer_reply", columnDefinition = "TEXT")
    private String freelancerReply;

    /**
     * Indique si l'avis a été flaggé comme suspect
     * (ex: incohérence note/commentaire).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean flagged = false;


    @Column(name = "flag_reason", length = 255)
    private String flagReason;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.VISIBLE;


    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private LocalDateTime reviewedAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;

    /** Sentiment prédit par le modèle ML : "POSITIVE", "NEGATIVE", "UNKNOWN" */
    @Column(name = "sentiment_label", length = 20)
    private String sentimentLabel;

    /** Score de confiance du modèle ML (entre 0.0 et 1.0) */
    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (reviewedAt == null) {
            reviewedAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = ReviewStatus.VISIBLE;
        }
        if (flagged == null) {
            flagged = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}