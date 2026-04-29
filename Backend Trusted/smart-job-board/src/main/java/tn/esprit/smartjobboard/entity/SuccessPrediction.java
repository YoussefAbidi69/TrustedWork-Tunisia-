package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Heuristic success probability for a freelancer on a specific job.
 */
@Entity
@Table(name = "success_predictions", indexes = {
        @Index(name = "idx_pred_job_freelancer", columnList = "job_offer_id,freelancer_id")
})
@Getter
@Setter
public class SuccessPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_offer_id", nullable = false)
    private Long jobOfferId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(nullable = false)
    private double probability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PredictionConfidence confidence;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        computedAt = LocalDateTime.now();
    }
}
