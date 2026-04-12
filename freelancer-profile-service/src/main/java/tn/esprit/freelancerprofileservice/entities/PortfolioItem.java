package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Projet réalisé par le freelancer — preuve de compétence pour SkillAuthenticity
 */
@Entity
@Table(name = "portfolio_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String projectUrl;  // Lien GitHub ou démo
    private String imageUrl;    // Capture d'écran du projet

    // Technologies utilisées (stockées en JSON string simple)
    @Column(columnDefinition = "TEXT")
    private String technologies; // Ex: "Spring Boot, Angular, MySQL"

    private LocalDate completionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;
}