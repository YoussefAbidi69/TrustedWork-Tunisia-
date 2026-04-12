package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Validation d'une compétence par un autre utilisateur
 * Anti-spam : 1 endorsement maximum par user par skill (contrainte DB)
 */
@Entity
@Table(
        name = "endorsements",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"endorser_id", "skill_id"},
                name = "uk_endorser_skill" // Un seul endorsement par (user, skill)
        )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Endorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID de l'utilisateur qui valide (référence Module 01)
    @Column(name = "endorser_id", nullable = false)
    private Long endorserId;

    @Column(length = 500)
    private String comment;

    private LocalDateTime endorsedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}