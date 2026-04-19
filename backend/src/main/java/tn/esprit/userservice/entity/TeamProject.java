package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Entity
@Table(name = "team_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long contractId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1500)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(nullable = false)
    private Integer progress;

    @Column(nullable = false)
    private Boolean active;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    private AgencyMember createdByMember;



    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ProjectStatus.PLANNED;
        }

        if (this.progress == null) {
            this.progress = 0;
        }

        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    //Relation ManyToOne avec l’entité Agency. Un projet appartient à une seule agence, mais une agence peut avoir plusieurs projets.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    //Relation OneToMany avec l’entité Task. Un projet peut avoir plusieurs tâches, mais une tâche appartient à un seul projet.
    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
}