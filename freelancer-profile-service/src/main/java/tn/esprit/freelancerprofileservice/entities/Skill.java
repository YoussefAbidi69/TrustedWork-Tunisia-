package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;

import java.util.List;

/**
 * Compétence technique du freelancer
 * Le niveau peut être auto-upgradé par SkillAuthenticityService
 */
@Entity
@Table(name = "skills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Ex: "Spring Boot", "Angular", "Docker"

    @Enumerated(EnumType.STRING)
    private SkillLevel level = SkillLevel.JUNIOR;

    // Score d'authenticité (0.0 - 1.0) calculé par SkillAuthenticityService
    // score = (portfolioEvidence * 0.40) + (examScore * 0.35) + (endorsements * 0.25)
    private Double authenticityScore = 0.0;

    private Double examScore = 0.0; // Score examen interne (Module 04)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Endorsement> endorsements;
}