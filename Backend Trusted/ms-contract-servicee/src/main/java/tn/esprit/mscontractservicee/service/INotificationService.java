package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.entity.Notification;
import tn.esprit.mscontractservicee.enums.NotificationType;

import java.util.List;

public interface INotificationService {
    
    // Créer une nouvelle notification
    Notification createNotification(Long recipientCin, String title, String message, NotificationType type, String relatedUrl);
    
    // Récupérer toutes les notifications d'un utilisateur
    List<Notification> getUserNotifications(Long recipientCin);
    
    // Récupérer uniquement les notifications non lues
    List<Notification> getUnreadNotifications(Long recipientCin);
    
    // Compter les notifications non lues
    long countUnread(Long recipientCin);
    
    // Marquer une notification comme lue
    void markAsRead(Long notificationId);
    
    // Marquer toutes les notifications d'un utilisateur comme lues
    void markAllAsRead(Long recipientCin);
}
