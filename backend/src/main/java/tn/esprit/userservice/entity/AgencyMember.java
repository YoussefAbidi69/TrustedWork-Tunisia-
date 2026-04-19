package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(
    name = "agency_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_agency",
        columnNames = {"user_id", "agency_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Proper FK to User (replaces bare Long userId) ─────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ── Agency-scoped role: only LEAD or MEMBER stored in DB ──────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    // ── Active status replaces Boolean active field ────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    // Legacy fallback column
    @Column(nullable = true)
    private Boolean active;

    @Column(nullable = false)
    private Float workloadScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(length = 1000)
    private String skills;

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = MemberRole.MEMBER;
        }
        if (this.workloadScore == null) {
            this.workloadScore = 0f;
        }
        if (this.status == null) {
            this.status = MemberStatus.ACTIVE;
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    // ── Relation: Many AgencyMembers → One Agency ──────────────────────────────
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    // ── Relation: One AgencyMember → many TaskAssignments ─────────────────────
    @JsonIgnore
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAssignment> assignments = new ArrayList<>();
}