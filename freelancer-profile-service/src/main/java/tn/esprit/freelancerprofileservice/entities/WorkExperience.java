package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Expérience professionnelle du freelancer
 */
@Entity
@Table(name = "work_experiences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobTitle; // Ex: "Développeur Backend"

    @Column(nullable = false)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate; // Null si poste actuel

    private Boolean isCurrent = false; // Poste actuel

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;
}