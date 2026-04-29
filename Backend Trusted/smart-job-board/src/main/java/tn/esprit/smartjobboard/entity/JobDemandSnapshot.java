package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Captures same-category job volume used when computing opportunity demand component.
 */
@Entity
@Table(name = "job_demand_snapshots", indexes = @Index(name = "idx_demand_job", columnList = "job_offer_id"))
@Getter
@Setter
public class JobDemandSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_offer_id", nullable = false)
    private Long jobOfferId;

    @Column(nullable = false, length = 120)
    private String category;

    @Column(name = "jobs_same_category_30d", nullable = false)
    private int jobsSameCategory30d;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        recordedAt = LocalDateTime.now();
    }
}
