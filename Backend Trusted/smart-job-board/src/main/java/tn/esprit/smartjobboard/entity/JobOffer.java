package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core job posting owned by a client user (FK stored as Long user id, validated via user-service).
 */
@Entity
@Table(name = "job_offers", indexes = {
        @Index(name = "idx_job_client", columnList = "client_id"),
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_category", columnList = "category"),
        @Index(name = "idx_job_published", columnList = "published_at")
})
@Getter
@Setter
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 120)
    private String category;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_offer_required_skills", joinColumns = @JoinColumn(name = "job_offer_id"))
    @Column(name = "skill")
    private List<String> requiredSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_offer_extracted_skills", joinColumns = @JoinColumn(name = "job_offer_id"))
    @Column(name = "skill")
    private List<String> extractedSkills = new ArrayList<>();

    @Column(name = "budget_min", precision = 14, scale = 2, nullable = false)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 14, scale = 2, nullable = false)
    private BigDecimal budgetMax;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(length = 255)
    private String location;

    @Column(name = "is_remote", nullable = false)
    private boolean remote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobOfferStatus status = JobOfferStatus.DRAFT;

    @Column(name = "fraud_risk_score", nullable = false)
    private double fraudRiskScore = 0.0;

    @Column(name = "opportunity_score", nullable = false)
    private double opportunityScore = 0.0;

    @Column(name = "opportunity_budget_component")
    private Double opportunityBudgetComponent;

    @Column(name = "opportunity_demand_component")
    private Double opportunityDemandComponent;

    @Column(name = "opportunity_competition_component")
    private Double opportunityCompetitionComponent;

    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;

    @Column(name = "opportunity_agent_processed_at")
    private LocalDateTime opportunityAgentProcessedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        createdAt = n;
        updatedAt = n;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
