package tn.esprit.msprojectservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.NotificationDTO;
import tn.esprit.msprojectservice.entities.Notification;
import tn.esprit.msprojectservice.entities.NotificationType;
import tn.esprit.msprojectservice.repositories.INotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private INotificationRepository notificationRepository;

    @Override
    public void createNotification(Long userId, String title, String message, NotificationType type, Long projectId, Long taskId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .projectId(projectId)
                .taskId(taskId)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<NotificationDTO> getAllNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    public NotificationDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée avec l'id : " + id));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(updated);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}