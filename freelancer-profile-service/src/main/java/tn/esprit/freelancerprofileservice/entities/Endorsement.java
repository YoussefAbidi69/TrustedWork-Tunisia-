package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Validation d'une compétence par un autre utilisateur.
 * Un utilisateur ne peut endors­er qu'une seule fois le même skill.
 */
@Entity
@Table(
        name = "endorsements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_endorser_skill",
                columnNames = {"endorser_id", "skill_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endorsement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endorser_id", nullable = false)
    private Long endorserId;

    @Size(max = 500)
    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDateTime endorsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @PrePersist
    public void prePersist() {
        if (comment != null) {
            comment = comment.trim();
            if (comment.isBlank()) {
                comment = null;
            }
        }

        if (endorsedAt == null) {
            endorsedAt = LocalDateTime.now();
        }
    }
}