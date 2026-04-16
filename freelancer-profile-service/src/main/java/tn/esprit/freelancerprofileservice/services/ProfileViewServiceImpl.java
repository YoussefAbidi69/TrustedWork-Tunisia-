package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.dto.response.ProfileViewAnalyticsResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Notification;
import tn.esprit.freelancerprofileservice.entities.ProfileView;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileViewRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implémentation du service d'analytics de visites
 */
@Service
@RequiredArgsConstructor
public class ProfileViewServiceImpl implements IProfileViewService {

    private final ProfileViewRepository viewRepository;
    private final FreelancerProfileRepository profileRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void recordView(Long profileId, Long viewerId) {
        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable avec id : " + profileId));

        // Empêcher qu’un freelancer augmente lui-même son compteur
        if (viewerId != null && viewerId.equals(profile.getUserId())) {
            return;
        }

        // Anti-spam : une seule vue par heure pour le même viewer sur le même profil
        if (viewerId != null) {
            Optional<ProfileView> lastViewOpt =
                    viewRepository.findTopByProfileIdAndViewerIdOrderByViewedAtDesc(profileId, viewerId);

            if (lastViewOpt.isPresent()) {
                LocalDateTime lastViewedAt = lastViewOpt.get().getViewedAt();
                if (lastViewedAt != null && lastViewedAt.isAfter(LocalDateTime.now().minusHours(1))) {
                    return;
                }
            }
        }

        ProfileView view = ProfileView.builder()
                .profile(profile)
                .viewerId(viewerId)
                .viewedAt(LocalDateTime.now())
                .build();

        viewRepository.save(view);

        Integer currentViews = profile.getTotalViews() == null ? 0 : profile.getTotalViews();
        profile.setTotalViews(currentViews + 1);
        profileRepository.save(profile);

        persisterEtEnvoyerNotificationVue(profileId, profile.getUserId(), viewerId);
    }

    @Override
    public long getTotalViews(Long profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("Profil introuvable avec id : " + profileId);
        }
        return viewRepository.countByProfileId(profileId);
    }

    @Override
    public ProfileViewAnalyticsResponse getAnalytics(Long profileId) {
        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable avec id : " + profileId));

        long totalViews = viewRepository.countByProfileId(profileId);
        long uniqueViewers = viewRepository.countDistinctViewersByProfileId(profileId);
        long viewsLast7Days = viewRepository.countViewsSince(profileId, LocalDateTime.now().minusDays(7));

        // On garde totalViews de l'entité synchronisé avec la réalité DB
        Integer entityViews = profile.getTotalViews() == null ? 0 : profile.getTotalViews();
        if (entityViews.longValue() != totalViews) {
            profile.setTotalViews((int) totalViews);
            profileRepository.save(profile);
        }

        return ProfileViewAnalyticsResponse.builder()
                .profileId(profileId)
                .totalViews(totalViews)
                .uniqueViewers(uniqueViewers)
                .viewsLast7Days(viewsLast7Days)
                .build();
    }

    /**
     * Persiste puis envoie en temps réel une notification lorsqu'un profil reçoit une nouvelle vue valide.
     */
    private void persisterEtEnvoyerNotificationVue(Long profileId, Long freelancerUserId, Long viewerId) {
        String payload = "{\"profileId\":" + profileId +
                ",\"viewerId\":" + (viewerId != null ? viewerId : "null") + "}";

        Notification notification = Notification.builder()
                .userId(freelancerUserId)
                .type("PROFILE_VIEW")
                .message("Votre profil a reçu une nouvelle vue.")
                .payload(payload)
                .read(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        messagingTemplate.convertAndSend(
                "/topic/user/" + freelancerUserId + "/notifications",
                savedNotification
        );
    }
}