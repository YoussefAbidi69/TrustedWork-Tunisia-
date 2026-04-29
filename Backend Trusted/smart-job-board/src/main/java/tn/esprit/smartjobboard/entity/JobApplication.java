package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Freelancer application to a job offer; unique per (job, freelancer).
 */
@Entity
@Table(name = "job_applications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_freelancer", columnNames = {"job_offer_id", "freelancer_id"})
}, indexes = {
        @Index(name = "idx_app_job", columnList = "job_offer_id"),
        @Index(name = "idx_app_freelancer", columnList = "freelancer_id")
})
@Getter
@Setter
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "cover_letter", nullable = false, columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "proposed_rate", precision = 14, scale = 2, nullable = false)
    private BigDecimal proposedRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        if (appliedAt == null) {
            appliedAt = n;
        }
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
