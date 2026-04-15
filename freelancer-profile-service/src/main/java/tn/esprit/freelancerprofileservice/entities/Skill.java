package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.SkillCategory;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compétence technique du freelancer.
 */
@Entity
@Table(
        name = "skills",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"profile_id", "normalized_name"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 2, max = 50)
    @Column(nullable = false, length = 50)
    private String name; // Ex: Spring Boot, Angular, Docker

    @Column(name = "normalized_name", nullable = false, length = 50)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SkillLevel level = SkillLevel.JUNIOR;

    // Score sur 100 pour être plus lisible côté jury / UI
    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Double authenticityScore = 0.0;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Double examScore = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer endorsementCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Endorsement> endorsements = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void normalizeFields() {
        if (name != null) {
            name = name.trim().replaceAll("\\s+", " ");
            normalizedName = name.toLowerCase(Locale.ROOT);
        }

        if (level == null) {
            level = SkillLevel.JUNIOR;
        }

        if (authenticityScore == null) {
            authenticityScore = 0.0;
        }

        if (examScore == null) {
            examScore = 0.0;
        }

        if (endorsementCount == null) {
            endorsementCount = 0;
        }
    }
}