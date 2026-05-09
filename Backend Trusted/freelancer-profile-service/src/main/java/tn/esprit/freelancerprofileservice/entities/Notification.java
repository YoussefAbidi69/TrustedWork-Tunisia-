package tn.esprit.freelancerprofileservice.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification persistée en base pour un utilisateur.
 * Créée lors d'un événement métier (nouvel avis, endorsement, signalement...).
 * Récupérée au login via GET /api/notifications/user/{userId}/unread
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Destinataire de la notification (userId du freelancer)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Type : NEW_REVIEW, NEW_ENDORSEMENT, NEW_REPORT, REPORT_RESOLVED...
    @Column(nullable = false, length = 50)
    private String type;

    // Message affiché dans la cloche
    @Column(nullable = false)
    private String message;

    // Données supplémentaires en JSON (ex: profileId, rating...)
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    // false = non lue, true = lue
    // Renommé de 'read' → 'isRead' : 'read' est un mot réservé en JPQL (Hibernate 6)
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (isRead == null)    isRead = false;
    }
}