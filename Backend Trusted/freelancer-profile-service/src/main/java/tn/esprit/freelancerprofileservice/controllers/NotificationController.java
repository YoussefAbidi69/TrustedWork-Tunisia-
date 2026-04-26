package tn.esprit.freelancerprofileservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.response.NotificationResponse;
import tn.esprit.freelancerprofileservice.entities.Notification;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;

import java.util.List;

/**
 * Controller REST — gestion des notifications persistées.
 *
 * Endpoints :
 * - GET  /api/notifications/user/{userId}/unread  → notifications non lues
 * - GET  /api/notifications/user/{userId}/count   → nombre de non-lues
 * - PUT  /api/notifications/user/{userId}/read-all → marquer tout comme lu
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    /**
     * Récupère les notifications non lues d'un utilisateur.
     * Appelé au login et au connect() WebSocket.
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(@PathVariable Long userId) {
        List<NotificationResponse> notifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(notifications);
    }

    /**
     * Retourne uniquement le nombre de notifications non lues (badge).
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    /**
     * Marque toutes les notifications comme lues (appelé au clic sur la cloche).
     */
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .payload(n.getPayload())
                .read(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}