package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "agency_performance_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyPerformanceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Float deliveryRate;

    @Column(nullable = false)
    private Float clientSatisfaction;

    @Column(nullable = false)
    private Float responseTime;

    @Column(nullable = false)
    private Float memberRetention;

    @Column(nullable = false)
    private Float totalScore;

    @Column(nullable = false)
    private LocalDateTime computedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false, unique = true)
    private Agency agency;

    @PrePersist
    public void prePersist() {
        if (this.computedAt == null) {
            this.computedAt = LocalDateTime.now();
        }

        if (this.deliveryRate == null) {
            this.deliveryRate = 0f;
        }

        if (this.clientSatisfaction == null) {
            this.clientSatisfaction = 0f;
        }

        if (this.responseTime == null) {
            this.responseTime = 0f;
        }

        if (this.memberRetention == null) {
            this.memberRetention = 0f;
        }

        if (this.totalScore == null) {
            this.totalScore = 0f;
        }
    }
}