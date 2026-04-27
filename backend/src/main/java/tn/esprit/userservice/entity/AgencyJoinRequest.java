package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * A freelancer-initiated join request to a specific agency.
 * Flow: Freelancer sends request → Owner accepts or declines.
 * This is the OPPOSITE direction from AgencyInvitation (owner → freelancer).
 */
@Entity
@Table(
    name = "agency_join_requests",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_join_request_user_agency",
        columnNames = {"requester_id", "agency_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The freelancer who wants to join
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // The agency being requested to join
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime respondedAt;

    @PrePersist
    public void prePersist() {
        if (this.requestedAt == null) this.requestedAt = LocalDateTime.now();
        if (this.status == null) this.status = JoinRequestStatus.PENDING;
    }
}
