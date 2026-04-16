package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.dto.request.AddReviewRequest;
import tn.esprit.freelancerprofileservice.dto.request.ReplyToReviewRequest;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReviewSummaryResponse;
import tn.esprit.freelancerprofileservice.dto.response.ReviewResponse;
import tn.esprit.freelancerprofileservice.dto.websocket.ReviewNotificationMessage;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Notification;
import tn.esprit.freelancerprofileservice.entities.ProfileReview;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReviewRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implémentation du service des avis clients.
 * Lors d'un nouvel avis :
 *   1. Sauvegarde en MySQL (persistant — visible après reconnexion)
 *   2. Envoi WebSocket temps réel (si le freelancer est connecté)
 */
@Service
@RequiredArgsConstructor
@Slf4j

public class ProfileReviewServiceImpl implements IProfileReviewService {

    private final ProfileReviewRepository reviewRepository;
    private final FreelancerProfileRepository profileRepository;
    private final ReviewAnalysisService reviewAnalysisService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final MlServiceClient mlServiceClient;


    @Override
    public ReviewResponse addReview(Long profileId, AddReviewRequest request) {
        // Vérification existence du profil
        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", profileId));

        // Un freelancer ne peut pas s'auto-évaluer
        if (profile.getUserId() != null && profile.getUserId().equals(request.getClientId())) {
            throw new InvalidDataException("Vous ne pouvez pas laisser un avis sur votre propre profil");
        }

        // Un client ne peut laisser qu'un seul avis par profil
        if (reviewRepository.existsByClientIdAndProfileId(request.getClientId(), profileId)) {
            throw new DuplicateResourceException("Vous avez déjà laissé un avis sur ce profil");
        }

        // Validation de la note
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new InvalidDataException("La note doit être entre 1 et 5");
        }

        // -------------------------------------------------------
        // Analyse ML du sentiment via micro-service Python Flask
        // Remplace l'ancienne détection par mots-clés statiques
        // -------------------------------------------------------
        boolean flagged = false;
        String flagReason = null;

        MlServiceClient.SentimentResult sentiment =
                mlServiceClient.predictSentiment(request.getComment());

        if (!"UNKNOWN".equals(sentiment.sentiment())) {
            // Incohérence : note haute (4-5) avec commentaire négatif
            if (request.getRating() >= 4 && "NEGATIVE".equals(sentiment.sentiment())) {
                flagged = true;
                flagReason = "ML_INCONSISTENT_HIGH_RATING_NEGATIVE_SENTIMENT";
            }
            // Incohérence : note basse (1-2) avec commentaire positif
            if (request.getRating() <= 2 && "POSITIVE".equals(sentiment.sentiment())) {
                flagged = true;
                flagReason = "ML_INCONSISTENT_LOW_RATING_POSITIVE_SENTIMENT";
            }
        } else {
            // Service ML indisponible — fallback sur l'ancienne logique mots-clés
            log.warn("[ML] Fallback vers analyse par mots-clés pour profileId={}", profileId);
            ReviewAnalysisResult analysisResult =
                    reviewAnalysisService.analyze(request.getRating(), request.getComment());
            flagged = analysisResult.isFlagged();
            flagReason = analysisResult.getFlagReason();
        }

        // Construction et sauvegarde de l'avis
        ProfileReview review = ProfileReview.builder()
                .clientId(request.getClientId())
                .profile(profile)
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.VISIBLE)
                .flagged(flagged)
                .flagReason(flagReason)
                .build();

        ProfileReview savedReview = reviewRepository.save(review);

        // Notifications persistée + WebSocket
        persisterNotification(profile, savedReview);
        envoyerNotificationWebSocket(profile, savedReview);

        return mapToResponse(savedReview);
    }

    /**
     * Sauvegarde la notification en base MySQL.
     * Récupérée au prochain login via GET /api/notifications/user/{userId}/unread
     */
    private void persisterNotification(FreelancerProfile profile, ProfileReview review) {
        if (profile.getUserId() == null) return;

        String message = "Vous avez reçu un nouvel avis " + review.getRating() + "★"
                + (review.getFlagged() ? " (signalé comme suspect)" : "");

        Notification notification = Notification.builder()
                .userId(profile.getUserId())
                .type("NEW_REVIEW")
                .message(message)
                .payload("{\"profileId\":" + profile.getId()
                        + ",\"rating\":" + review.getRating()
                        + ",\"flagged\":" + review.getFlagged() + "}")
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Publie un message WebSocket sur le topic personnel du freelancer.
     * Reçu instantanément si le freelancer est connecté au moment de l'envoi.
     */
    private void envoyerNotificationWebSocket(FreelancerProfile profile, ProfileReview review) {
        if (profile.getUserId() == null) return;

        String topic = "/topic/user/" + profile.getUserId() + "/notifications";

        ReviewNotificationMessage message = ReviewNotificationMessage.builder()
                .type("NEW_REVIEW")
                .profileId(profile.getId())
                .clientId(review.getClientId())
                .rating(review.getRating())
                .flagged(review.getFlagged())
                .message("Vous avez reçu un nouvel avis " + review.getRating() + "★")
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        messagingTemplate.convertAndSend(topic, message);
    }

    @Override
    public List<ReviewResponse> getVisibleReviews(Long profileId) {
        ensureProfileExists(profileId);
        return reviewRepository
                .findByProfileIdAndStatusOrderByReviewedAtDesc(profileId, ReviewStatus.VISIBLE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Double getAverageRating(Long profileId) {
        ensureProfileExists(profileId);
        Double avg = reviewRepository.findAverageRatingByProfileId(profileId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public ProfileReviewSummaryResponse getReviewSummary(Long profileId) {
        ensureProfileExists(profileId);

        long totalReviews = reviewRepository.countByProfileIdAndStatus(profileId, ReviewStatus.VISIBLE);
        double averageRating = getAverageRating(profileId);
        long fiveStarCount = reviewRepository.countByProfileIdAndRatingAndStatus(profileId, 5, ReviewStatus.VISIBLE);
        long fourStarCount = reviewRepository.countByProfileIdAndRatingAndStatus(profileId, 4, ReviewStatus.VISIBLE);
        long threeStarCount = reviewRepository.countByProfileIdAndRatingAndStatus(profileId, 3, ReviewStatus.VISIBLE);
        long twoStarCount = reviewRepository.countByProfileIdAndRatingAndStatus(profileId, 2, ReviewStatus.VISIBLE);
        long oneStarCount = reviewRepository.countByProfileIdAndRatingAndStatus(profileId, 1, ReviewStatus.VISIBLE);

        return ProfileReviewSummaryResponse.builder()
                .profileId(profileId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .build();
    }

    @Override
    public ReviewResponse replyToReview(Long reviewId, Long freelancerUserId, ReplyToReviewRequest request) {
        ProfileReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis", reviewId));

        FreelancerProfile profile = review.getProfile();

        if (profile.getUserId() == null || !profile.getUserId().equals(freelancerUserId)) {
            throw new InvalidDataException("Vous n'êtes pas autorisé à répondre à cet avis");
        }

        review.setFreelancerReply(request.getReply());
        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    public void hideReview(Long reviewId) {
        ProfileReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis", reviewId));

        review.setStatus(ReviewStatus.HIDDEN);
        reviewRepository.save(review);
    }

    @Override
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Avis", reviewId);
        }

        reviewRepository.deleteById(reviewId);
    }

    private void ensureProfileExists(Long profileId) {
        if (!profileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("Profil", profileId);
        }
    }

    private ReviewResponse mapToResponse(ProfileReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .clientId(review.getClientId())
                .rating(review.getRating())
                .comment(review.getComment())
                .freelancerReply(review.getFreelancerReply())
                .flagged(review.getFlagged())
                .flagReason(review.getFlagReason())
                .status(review.getStatus())
                .reviewedAt(review.getReviewedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    @Override
    public ReviewResponse restoreReview(Long reviewId) {
        ProfileReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis", reviewId));

        review.setStatus(ReviewStatus.VISIBLE);

        ProfileReview saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }


}