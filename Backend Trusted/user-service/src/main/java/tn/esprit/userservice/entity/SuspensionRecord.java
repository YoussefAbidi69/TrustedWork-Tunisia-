package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "suspension_records")
public class SuspensionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Utilisateur suspendu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ================= SUSPENSION =================
    // Motif de la suspension
    @Column(nullable = false)
    private String reason;

    // Email de l'admin ayant suspendu
    @Column(name = "suspended_by", nullable = false)
    private String suspendedBy;

    // Date de la suspension
    @Column(name = "suspended_at", nullable = false, updatable = false)
    private LocalDateTime suspendedAt;

    // ================= LEVÉE =================
    // Date de levée de la suspension (null = toujours suspendu)
    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    // Email de l'admin ayant levé la suspension
    @Column(name = "lifted_by")
    private String liftedBy;

    // ================= STATUT =================
    // true = suspension active, false = levée
    @Column(nullable = false)
    private boolean active = true;

    @PrePersist
    public void prePersist() {
        this.suspendedAt = LocalDateTime.now();
    }
}