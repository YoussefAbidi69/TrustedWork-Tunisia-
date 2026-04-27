package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Parcours académique du freelancer
 */
@Entity
@Table(name = "educations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String degree; // Ex: "Licence en Informatique"

    @Column(nullable = false)
    private String institution; // Ex: "ESPRIT"

    private String fieldOfStudy; // Ex: "Génie Logiciel"

    private Integer graduationYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;
}