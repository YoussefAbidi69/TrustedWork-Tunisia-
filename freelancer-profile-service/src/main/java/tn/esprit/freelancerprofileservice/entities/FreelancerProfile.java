package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.enums.ProfileVisibility;
import tn.esprit.freelancerprofileservice.enums.ProjectType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité principale du profil freelancer
 * Référence l'userId du Module 01 (user-service) — pas de FK cross-service
 */
@Entity
@Table(name = "freelancer_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FreelancerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence vers user-service (Module 01) — ID uniquement, pas de @ManyToOne
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 100)
    private String headline; // Ex: "Développeur Spring Boot Senior"

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String avatarUrl;

    private Double hourlyRate; // Taux horaire en TND

    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    private ProjectType projectType = ProjectType.BOTH;

    // Score de complétude du profil (0-100) — calculé par CompletenessService
    private Integer completenessScore = 0;

    // Classement régional (gouvernorat tunisien)
    private String region;
    private Integer regionalRank;

    // Nombre total de vues du profil
    private Integer totalViews = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Relations OneToMany — cascade delete propre
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioItem> portfolioItems;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkExperience> workExperiences;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileReview> reviews;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileReport> reports;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileView> views;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}