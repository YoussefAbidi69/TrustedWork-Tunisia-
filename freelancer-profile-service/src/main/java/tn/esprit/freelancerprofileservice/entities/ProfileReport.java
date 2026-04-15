package tn.esprit.freelancerprofileservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.time.LocalDateTime;

/**
 * Signalement d'un profil freelancer frauduleux, inapproprié ou suspect.
 * Utilisé dans le workflow de modération admin.
 */
@Entity
@Table(
        name = "profile_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_profile_report_reporter_profile",
                        columnNames = {"reporter_id", "profile_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportCategory category;


    @Column(nullable = false, length = 1000)
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Profil concerné par le signalement.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private FreelancerProfile profile;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}