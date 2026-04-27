package tn.esprit.smartjobboard.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Single triggered fraud or moderation signal attached to a job offer.
 */
@Entity
@Table(name = "offer_flags", indexes = @Index(name = "idx_flag_job", columnList = "job_offer_id"))
@Getter
@Setter
public class OfferFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @Column(name = "signal_code", nullable = false, length = 64)
    private String signalCode;

    @Column(nullable = false, length = 512)
    private String message;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
