package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expérience professionnelle du freelancer
 */
@Entity
@Table(
        name = "work_experiences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_work_experience_profile_job_company_start",
                        columnNames = {"profile_id", "jobTitle", "company", "startDate"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String jobTitle;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(length = 150)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCurrent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private FreelancerProfile profile;

    /**
     * Dates techniques
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        normalizeCurrentFlag();
    }

    @PreUpdate
    public void preUpdate() {
        normalizeCurrentFlag();
    }

    private void normalizeCurrentFlag() {
        if (this.isCurrent == null) {
            this.isCurrent = false;
        }
        if (Boolean.TRUE.equals(this.isCurrent)) {
            this.endDate = null;
        }
    }
}