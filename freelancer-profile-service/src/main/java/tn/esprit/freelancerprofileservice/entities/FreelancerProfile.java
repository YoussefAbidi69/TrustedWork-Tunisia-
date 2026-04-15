package tn.esprit.freelancerprofileservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.enums.ProfileVisibility;
import tn.esprit.freelancerprofileservice.enums.ProjectType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité principale du profil freelancer
 */
@Entity
@Table(name = "freelancer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreelancerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 100)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String avatarUrl;

    private Double hourlyRate;

    @Enumerated(EnumType.STRING)
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    private ProfileVisibility visibility = ProfileVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    private ProjectType projectType = ProjectType.BOTH;

    private Integer completenessScore = 0;

    private String region;

    private Integer regionalRank;

    private Integer totalViews = 0;

    /**
     * Score de risque du profil basé sur le nombre de signalements.
     * 0 = aucun risque, 100 = risque maximal.
     */
    private Integer riskScore = 0;

    /**
     * Suspension automatique du profil après seuil de reports.
     */
    private Boolean suspended = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Skill> skills;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortfolioItem> portfolioItems;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkExperience> workExperiences;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileReview> reviews;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileReport> reports;

    @JsonIgnore
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProfileView> views;

    @PrePersist
    protected void onCreate() {
        if (completenessScore == null) {
            completenessScore = 0;
        }
        if (totalViews == null) {
            totalViews = 0;
        }
        if (riskScore == null) {
            riskScore = 0;
        }
        if (suspended == null) {
            suspended = false;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (riskScore == null) {
            riskScore = 0;
        }
        if (suspended == null) {
            suspended = false;
        }
        updatedAt = LocalDateTime.now();
    }
}