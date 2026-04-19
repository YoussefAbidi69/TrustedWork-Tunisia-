package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "agency_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── The LEAD who sent this invitation ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // ── The User being invited ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // ── Role proposed for the receiver (always MEMBER for invitations) ────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole proposedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime respondedAt;

    @Column(length = 500)
    private String message;

    @PrePersist
    public void prePersist() {
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = InvitationStatus.PENDING;
        }
        if (this.proposedRole == null) {
            this.proposedRole = MemberRole.MEMBER;
        }
    }

    // ── Relation: Many invitations → One Agency ───────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;
}