package tn.esprit.mscontractservicee.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tn.esprit.mscontractservicee.entity.Notification;
import tn.esprit.mscontractservicee.enums.NotificationType;
import tn.esprit.mscontractservicee.service.INotificationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private Notification buildNotif(Long id, Long cin) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientCin(cin);
        n.setTitle("Test");
        n.setMessage("Test message");
        n.setType(NotificationType.INFO);
        n.setRead(false);
        return n;
    }

    @Test
    void testGetUnreadNotifications() {
        List<Notification> notifications = Arrays.asList(buildNotif(1L, 123L), buildNotif(2L, 123L));
        when(notificationService.getUnreadNotifications(123L)).thenReturn(notifications);

        ResponseEntity<List<Notification>> response = notificationController.getUnreadNotifications(123L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testGetMyNotifications() {
        List<Notification> notifications = List.of(buildNotif(1L, 123L));
        when(notificationService.getUserNotifications(123L)).thenReturn(notifications);

        ResponseEntity<List<Notification>> response = notificationController.getMyNotifications(123L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testCountUnread() {
        when(notificationService.countUnread(123L)).thenReturn(5L);

        ResponseEntity<Long> response = notificationController.countUnread(123L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(5L, response.getBody());
    }

    @Test
    void testMarkAsRead() {
        doNothing().when(notificationService).markAsRead(1L);

        ResponseEntity<Void> response = notificationController.markAsRead(1L);

        assertEquals(200, response.getStatusCode().value());
        verify(notificationService, times(1)).markAsRead(1L);
    }

    @Test
    void testMarkAllAsRead() {
        doNothing().when(notificationService).markAllAsRead(123L);

        ResponseEntity<Void> response = notificationController.markAllAsRead(123L);

        assertEquals(200, response.getStatusCode().value());
        verify(notificationService, times(1)).markAllAsRead(123L);
    }

    @Test
    void testGetUnreadNotifications_Empty() {
        when(notificationService.getUnreadNotifications(999L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<Notification>> response = notificationController.getUnreadNotifications(999L);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());
    }
}
