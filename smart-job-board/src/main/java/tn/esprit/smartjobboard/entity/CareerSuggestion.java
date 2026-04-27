package tn.esprit.smartjobboard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_suggestions", indexes = {
        @Index(name = "idx_career_freelancer", columnList = "freelancer_id")
})
@Getter
@Setter
public class CareerSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "suggested_skill", nullable = false, length = 120)
    private String suggestedSkill;

    @Column(name = "trend_score", nullable = false)
    private double trendScore;

    @Column(name = "co_occurrence_rate", nullable = false)
    private double coOccurrenceRate;

    @Column(name = "total_score", nullable = false)
    private double totalScore;

    @Column(name = "estimated_income_impact", nullable = false)
    private double estimatedIncomeImpact;

    @Column(name = "trend")
    private String trend;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}

