package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.NotificationDTO;
import tn.esprit.msprojectservice.entities.NotificationType;

import java.util.List;

public interface INotificationService {

    void createNotification(Long userId, String title, String message, NotificationType type, Long projectId, Long taskId);

    List<NotificationDTO> getAllNotifications(Long userId);

    List<NotificationDTO> getUnreadNotifications(Long userId);

    int getUnreadCount(Long userId);

    NotificationDTO markAsRead(Long id);

    void markAllAsRead(Long userId);
}