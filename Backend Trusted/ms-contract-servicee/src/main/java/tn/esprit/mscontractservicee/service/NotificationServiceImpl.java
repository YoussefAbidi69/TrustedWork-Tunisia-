package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.mscontractservicee.entity.Notification;
import tn.esprit.mscontractservicee.enums.NotificationType;
import tn.esprit.mscontractservicee.repository.NotificationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification createNotification(Long recipientCin, String title, String message, NotificationType type, String relatedUrl) {
        log.info("Création d'une notification pour CIN {}: {}", recipientCin, title);
        Notification notification = Notification.builder()
                .recipientCin(recipientCin)
                .title(title)
                .message(message)
                .type(type)
                .relatedUrl(relatedUrl)
                .read(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getUserNotifications(Long recipientCin) {
        return notificationRepository.findByRecipientCinOrderByCreatedAtDesc(recipientCin);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long recipientCin) {
        return notificationRepository.findByRecipientCinAndReadFalseOrderByCreatedAtDesc(recipientCin);
    }

    @Override
    public long countUnread(Long recipientCin) {
        return notificationRepository.countByRecipientCinAndReadFalse(recipientCin);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    public void markAllAsRead(Long recipientCin) {
        List<Notification> unreadList = notificationRepository.findByRecipientCinAndReadFalseOrderByCreatedAtDesc(recipientCin);
        unreadList.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unreadList);
    }
}
