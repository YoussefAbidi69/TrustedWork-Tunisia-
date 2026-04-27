package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot of budget positioning versus platform mock average for analytics and audits.
 */
@Entity
@Table(name = "budget_intelligence")
@Getter
@Setter
public class BudgetIntelligence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_offer_id", nullable = false, unique = true)
    private Long jobOfferId;

    @Column(name = "reference_average", precision = 14, scale = 2, nullable = false)
    private BigDecimal referenceAverage;

    @Column(name = "budget_max", precision = 14, scale = 2, nullable = false)
    private BigDecimal budgetMax;

    @Column(name = "normalized_budget_score", nullable = false)
    private double normalizedBudgetScore;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        computedAt = LocalDateTime.now();
    }
}
