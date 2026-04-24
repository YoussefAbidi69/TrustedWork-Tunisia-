package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Projet réalisé par le freelancer — preuve de compétence pour le portfolio
 */
@Entity
@Table(name = "portfolio_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String projectUrl;  // Lien GitHub, démo ou site

    @Column(length = 500)
    private String imageUrl;    // Capture ou image du projet

    @Column(columnDefinition = "TEXT")
    private String technologies; // Ex: "Spring Boot, Angular, MySQL"

    private LocalDate completionDate;

    @Column(nullable = false)
    private Boolean pinned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;

    @PrePersist
    public void prePersist() {
        if (pinned == null) {
            pinned = false;
        }
    }
}