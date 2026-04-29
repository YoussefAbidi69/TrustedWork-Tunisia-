package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Audit trail for autonomous opportunity-agent emails sent to freelancers.
 */
@Entity
@Table(name = "opportunity_notification_logs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notify_job_freelancer", columnNames = {"job_offer_id", "freelancer_id"})
})
@Getter
@Setter
public class OpportunityNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_offer_id", nullable = false)
    private Long jobOfferId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "match_score", nullable = false)
    private double matchScore;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        sentAt = LocalDateTime.now();
    }
}
