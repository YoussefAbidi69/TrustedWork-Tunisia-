package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity //Dit à JPA que cette classe doit être mappée à une table.
@Table(name = "agencies") //Permet de définir explicitement le nom de la table SQL.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agency {

    @Id //Identifiant primaire auto-généré.
    @GeneratedValue(strategy = GenerationType.IDENTITY) //Identifiant primaire auto-généré.
    private Long id;

    // Creator / Owner of this agency (immutable after creation)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false, updatable = false)
    private User createdBy;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 150)
    private String sector;

    @Column(length = 500)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgencyTier tier;

    // Legacy fallback column to satisfy MySQL NOT NULL constraint
    // Do not use in business logic; populated identically to createdBy in Service
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist //Permet de remplir automatiquement les dates et les valeurs par défaut.
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.tier == null) {
            this.tier = AgencyTier.STARTER;
        }

        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate //Permet de mettre à jour automatiquement la date de mise à jour.
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    //Relation avec les membres de l’agence. Un membre peut être dans plusieurs agences, mais ici on considère que c’est une relation unidirectionnelle.
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgencyMember> members = new ArrayList<>();

    //Relation avec les projets de l’agence. Un projet appartient à une seule agence, mais une agence peut avoir plusieurs projets.
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamProject> projects = new ArrayList<>();

    //Relation avec les logs de collaboration de l’agence. Un log de collaboration appartient à une seule agence, mais une agence peut avoir plusieurs logs de collaboration.
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollaborationLog> collaborationLogs = new ArrayList<>();

    //Relation avec le score de performance de l’agence. Un score de performance appartient à une seule agence, et une agence a un seul score de performance.
    @JsonIgnore
    @OneToOne(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private AgencyPerformanceScore performanceScore;


    //Relation avec l’analyse de couverture des compétences de l’agence. Une analyse de couverture des compétences appartient à une seule agence, mais une agence peut avoir plusieurs analyses de couverture des compétences (une par projet).

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkillCoverageAnalysis> skillCoverageAnalyses = new ArrayList<>();

    //Relation avec les invitations à rejoindre l’agence. Une invitation appartient à une seule agence, mais une agence peut avoir plusieurs invitations.
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgencyInvitation> invitations = new ArrayList<>();

    //Relation avec les avis sur l’agence. Un avis appartient à une seule agence, mais une agence peut avoir plusieurs avis.
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgencyReview> reviews = new ArrayList<>();

    // Relation avec les demandes d'adhésion
    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgencyJoinRequest> joinRequests = new ArrayList<>();

}
