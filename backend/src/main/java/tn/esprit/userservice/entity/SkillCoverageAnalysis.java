package tn.esprit.userservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "skill_coverage_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCoverageAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String coveredSkills;

    @Column(length = 2000)
    private String missingSkills;

    @Column(nullable = false)
    private Float coveragePercentage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime analyzedAt;



    @PrePersist
    public void prePersist() {
        if (this.coveragePercentage == null) {
            this.coveragePercentage = 0f;
        }

        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
    }


    // Relation ManyToOne avec Agency. Une analyse de couverture de compétences appartient à une seule agence, mais une agence peut avoir plusieurs analyses de couverture de compétences.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;
}
