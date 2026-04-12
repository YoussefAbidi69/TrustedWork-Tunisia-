package tn.esprit.msprojectservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRiskSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    private RiskType riskType;

    @Enumerated(EnumType.STRING)
    private RiskSeverity severity;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Long affectedTaskId;

    private boolean resolved;

    private LocalDateTime detectedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.detectedAt = LocalDateTime.now();
        this.resolved = false;
    }
}