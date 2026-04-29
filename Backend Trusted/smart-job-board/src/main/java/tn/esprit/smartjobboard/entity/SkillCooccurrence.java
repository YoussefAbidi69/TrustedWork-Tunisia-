package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Aggregated co-occurrence counts between skills observed across published job postings.
 */
@Entity
@Table(name = "skill_cooccurrences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_skill_pair", columnNames = {"skill_primary", "skill_related"})
})
@Getter
@Setter
public class SkillCooccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_primary", nullable = false, length = 120)
    private String skillPrimary;

    @Column(name = "skill_related", nullable = false, length = 120)
    private String skillRelated;

    @Column(nullable = false)
    private int coCount;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}
