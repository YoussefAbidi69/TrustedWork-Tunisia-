package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.mscontractservicee.entity.Notification;
import tn.esprit.mscontractservicee.enums.NotificationType;
import tn.esprit.mscontractservicee.repository.NotificationRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateNotification() {
        Long recipientCin = 12345L;
        String title = "Test";
        String message = "Message";
        NotificationType type = NotificationType.INFO;
        String url = "/test";

        Notification savedNotif = new Notification();
        savedNotif.setId(1L);
        savedNotif.setRecipientCin(recipientCin);
        savedNotif.setTitle(title);
        savedNotif.setMessage(message);
        savedNotif.setType(type);
        savedNotif.setRelatedUrl(url);
        savedNotif.setRead(false);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotif);

        Notification result = notificationService.createNotification(recipientCin, title, message, type, url);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(recipientCin, result.getRecipientCin());
        assertEquals(title, result.getTitle());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetUserNotifications() {
        Long recipientCin = 12345L;
        when(notificationRepository.findByRecipientCinOrderByCreatedAtDesc(recipientCin))
                .thenReturn(Arrays.asList(new Notification(), new Notification()));

        List<Notification> result = notificationService.getUserNotifications(recipientCin);

        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findByRecipientCinOrderByCreatedAtDesc(recipientCin);
    }

    @Test
    void testGetUnreadNotifications() {
        Long recipientCin = 12345L;
        when(notificationRepository.findByRecipientCinAndReadFalseOrderByCreatedAtDesc(recipientCin))
                .thenReturn(Arrays.asList(new Notification()));

        List<Notification> result = notificationService.getUnreadNotifications(recipientCin);

        assertEquals(1, result.size());
        verify(notificationRepository, times(1)).findByRecipientCinAndReadFalseOrderByCreatedAtDesc(recipientCin);
    }

    @Test
    void testCountUnread() {
        Long recipientCin = 12345L;
        when(notificationRepository.countByRecipientCinAndReadFalse(recipientCin)).thenReturn(5L);

        long count = notificationService.countUnread(recipientCin);

        assertEquals(5L, count);
        verify(notificationRepository, times(1)).countByRecipientCinAndReadFalse(recipientCin);
    }

    @Test
    void testMarkAsRead() {
        Notification notif = new Notification();
        notif.setId(1L);
        notif.setRead(false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        notificationService.markAsRead(1L);

        assertTrue(notif.isRead());
        verify(notificationRepository, times(1)).save(notif);
    }

    @Test
    void testMarkAllAsRead() {
        Long recipientCin = 12345L;
        Notification n1 = new Notification(); n1.setRead(false);
        Notification n2 = new Notification(); n2.setRead(false);
        List<Notification> unreadList = Arrays.asList(n1, n2);

        when(notificationRepository.findByRecipientCinAndReadFalseOrderByCreatedAtDesc(recipientCin))
                .thenReturn(unreadList);

        notificationService.markAllAsRead(recipientCin);

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        verify(notificationRepository, times(1)).saveAll(unreadList);
    }
}
