package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.NotificationDTO;
import tn.esprit.msprojectservice.scheduler.DeliveryRiskScheduler;
import tn.esprit.msprojectservice.services.INotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications utilisateur")
public class NotificationRestController {

    private final INotificationService   notificationService;
    private final DeliveryRiskScheduler  deliveryRiskScheduler;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Toutes les notifications", description = "Récupérer toutes les notifications d'un utilisateur")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getAllNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Notifications non lues", description = "Récupérer les notifications non lues d'un utilisateur")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread/count")
    @Operation(summary = "Compteur non lues", description = "Nombre de notifications non lues (pour le badge Angular)")
    public ResponseEntity<Integer> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer comme lue", description = "Marquer une notification comme lue")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PatchMapping("/user/{userId}/read-all")
    @Operation(summary = "Tout marquer comme lu", description = "Marquer toutes les notifications d'un utilisateur comme lues")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test/{projectId}")
    @Operation(summary = "Déclencher les notifications manuellement (test)")
    public ResponseEntity<String> testNotifications(@PathVariable Long projectId) {
        deliveryRiskScheduler.sendDailyNotifications();
        return ResponseEntity.ok("Notifications déclenchées pour le projet " + projectId);
    }
}