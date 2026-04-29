package tn.esprit.mscontractservicee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mscontractservicee.entity.Notification;
import tn.esprit.mscontractservicee.service.INotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications in-app pour les utilisateurs")
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping("/my-notifications")
    @Operation(summary = "Récupérer toutes les notifications de l'utilisateur connecté (via son CIN)")
    public ResponseEntity<List<Notification>> getMyNotifications(@RequestHeader("X-User-Cin") Long userCin) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userCin));
    }

    @GetMapping("/unread")
    @Operation(summary = "Récupérer uniquement les notifications non lues")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestHeader("X-User-Cin") Long userCin) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userCin));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Compter le nombre de notifications non lues")
    public ResponseEntity<Long> countUnread(@RequestHeader("X-User-Cin") Long userCin) {
        return ResponseEntity.ok(notificationService.countUnread(userCin));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marquer une notification spécifique comme lue")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User-Cin") Long userCin) {
        notificationService.markAllAsRead(userCin);
        return ResponseEntity.ok().build();
    }
}
