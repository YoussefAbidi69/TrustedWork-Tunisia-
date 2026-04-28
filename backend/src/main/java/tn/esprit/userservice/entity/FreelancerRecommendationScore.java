package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "freelancer_recommendation_scores", uniqueConstraints = {
        @UniqueConstraint(name = "uq_agency_freelancer", columnNames = {"agency_id", "freelancer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerRecommendationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(nullable = false)
    private Float recommendationScore;

    @Column(nullable = false)
    private Float skillMatchScore;

    @Column(nullable = false)
    private Float trustScore;

    @Column(nullable = false)
    private Float availabilityScore;

    @Column(nullable = false)
    private Float experienceScore;

    @Column(nullable = false)
    private Float similarityScore;

    @Column(nullable = false)
    private Float locationScore;

    @Column(length = 1000)
    private String explanation;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    public void updateComputedAt() {
        this.computedAt = LocalDateTime.now();
    }
}
