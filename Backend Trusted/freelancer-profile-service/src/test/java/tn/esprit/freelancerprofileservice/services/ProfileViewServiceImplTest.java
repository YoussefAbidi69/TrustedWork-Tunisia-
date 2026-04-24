package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.freelancerprofileservice.dto.response.ProfileViewAnalyticsResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.ProfileView;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileViewRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ProfileViewServiceImpl
 * Validation jury prévue : 18/04/2026
 */
@ExtendWith(MockitoExtension.class)
class ProfileViewServiceImplTest {

    @Mock
    private ProfileViewRepository viewRepository;

    @Mock
    private FreelancerProfileRepository profileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    // 🔥 NOUVEAU MOCK
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ProfileViewServiceImpl service;

    private FreelancerProfile profile;

    @BeforeEach
    void setup() {
        profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setUserId(100L);
        profile.setTotalViews(0);
    }

    // Validation jury 18/04/2026 — vue valide + notification + websocket
    @Test
    void shouldRecordViewSuccessfully() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(viewRepository.findTopByProfileIdAndViewerIdOrderByViewedAtDesc(1L, 200L))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordView(1L, 200L);

        verify(viewRepository, times(1)).save(any(ProfileView.class));
        verify(profileRepository, times(1)).save(profile);
        verify(notificationRepository, times(1)).save(any());

        // 🔥 vérification WebSocket

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/user/100/notifications"), any(Object.class));

        assertEquals(1, profile.getTotalViews());
    }

    // Validation jury — profil inexistant
    @Test
    void shouldThrowExceptionIfProfileNotFound() {
        when(profileRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.recordView(1L, 200L)
        );

        verify(viewRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // Validation jury — owner
    @Test
    void shouldNotCountViewIfViewerIsOwner() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        service.recordView(1L, 100L);

        verify(viewRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));

        assertEquals(0, profile.getTotalViews());
    }

    // Validation jury — anti-spam
    @Test
    void shouldNotRecordViewIfWithinOneHour() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        ProfileView lastView = ProfileView.builder()
                .viewerId(200L)
                .profile(profile)
                .viewedAt(LocalDateTime.now())
                .build();

        when(viewRepository.findTopByProfileIdAndViewerIdOrderByViewedAtDesc(1L, 200L))
                .thenReturn(Optional.of(lastView));

        service.recordView(1L, 200L);

        verify(viewRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // Validation jury — total views
    @Test
    void shouldReturnTotalViews() {
        when(profileRepository.existsById(1L)).thenReturn(true);
        when(viewRepository.countByProfileId(1L)).thenReturn(5L);

        long result = service.getTotalViews(1L);

        assertEquals(5L, result);
    }

    // Validation jury — exception count
    @Test
    void shouldThrowExceptionWhenGettingTotalViewsForUnknownProfile() {
        when(profileRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                service.getTotalViews(1L)
        );
    }

    // Validation jury — analytics
    @Test
    void shouldReturnAnalytics() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(viewRepository.countByProfileId(1L)).thenReturn(10L);
        when(viewRepository.countDistinctViewersByProfileId(1L)).thenReturn(7L);
        when(viewRepository.countViewsSince(eq(1L), any()))
                .thenReturn(3L);

        ProfileViewAnalyticsResponse analytics = service.getAnalytics(1L);

        assertNotNull(analytics);
        assertEquals(10L, analytics.getTotalViews());
        assertEquals(7L, analytics.getUniqueViewers());
        assertEquals(3L, analytics.getViewsLast7Days());
    }

    // Validation jury — sync totalViews
    @Test
    void shouldSyncProfileTotalViewsWhenAnalyticsDetectsDifference() {
        profile.setTotalViews(2);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(viewRepository.countByProfileId(1L)).thenReturn(10L);
        when(viewRepository.countDistinctViewersByProfileId(1L)).thenReturn(7L);
        when(viewRepository.countViewsSince(eq(1L), any()))
                .thenReturn(3L);

        service.getAnalytics(1L);

        assertEquals(10, profile.getTotalViews());
        verify(profileRepository, times(1)).save(profile);
    }
}