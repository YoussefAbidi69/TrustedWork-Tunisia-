package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persisted match breakdown between one job offer and one freelancer candidate.
 */
@Entity
@Table(name = "match_scores", uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_job_freelancer", columnNames = {"job_offer_id", "freelancer_id"})
}, indexes = {
        @Index(name = "idx_match_job_freelancer", columnList = "job_offer_id,freelancer_id")
})
@Getter
@Setter
public class MatchScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_offer_id", nullable = false)
    private Long jobOfferId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "skill_match", nullable = false)
    private double skillMatch;

    @Column(name = "reputation", nullable = false)
    private double reputation;

    @Column(name = "success_rate", nullable = false)
    private double successRate;

    @Column(name = "budget_fit", nullable = false)
    private double budgetFit;

    @Column(name = "availability", nullable = false)
    private double availability;

    @Column(name = "total_score", nullable = false)
    private double totalScore;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        computedAt = LocalDateTime.now();
    }
}
