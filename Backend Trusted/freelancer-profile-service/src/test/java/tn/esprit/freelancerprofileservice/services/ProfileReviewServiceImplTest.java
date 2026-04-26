package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.freelancerprofileservice.dto.request.AddReviewRequest;
import tn.esprit.freelancerprofileservice.dto.request.ReplyToReviewRequest;
import tn.esprit.freelancerprofileservice.dto.response.ReviewResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.ProfileReview;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReviewRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileReviewServiceImplTest {

    @Mock
    private ProfileReviewRepository reviewRepository;

    @Mock
    private FreelancerProfileRepository profileRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReviewAnalysisService reviewAnalysisService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MlServiceClient mlServiceClient;

    @InjectMocks
    private ProfileReviewServiceImpl service;

    private FreelancerProfile profile;

    @BeforeEach
    void setup() {
        profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setUserId(100L);
    }

    @Test
    void shouldAddReviewSuccessfully() {
        AddReviewRequest request = new AddReviewRequest();
        request.setClientId(200L);
        request.setRating(5);
        request.setComment("Great work");

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(reviewRepository.existsByClientIdAndProfileId(200L, 1L)).thenReturn(false);

        when(mlServiceClient.predictSentiment(any()))
                .thenReturn(new MlServiceClient.SentimentResult("POSITIVE", 0.95));

        when(reviewRepository.save(any())).thenAnswer(inv -> {
            ProfileReview r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReviewResponse response = service.addReview(1L, request);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(any());
        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/user/100/notifications"), any(Object.class));
    }

    @Test
    void shouldThrowWhenProfileNotFound() {
        when(profileRepository.findById(1L)).thenReturn(Optional.empty());

        AddReviewRequest request = new AddReviewRequest();
        request.setClientId(200L);
        request.setRating(5);

        assertThatThrownBy(() -> service.addReview(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowWhenSelfReview() {
        AddReviewRequest request = new AddReviewRequest();
        request.setClientId(100L);
        request.setRating(5);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.addReview(1L, request))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void shouldThrowWhenDuplicateReview() {
        AddReviewRequest request = new AddReviewRequest();
        request.setClientId(200L);
        request.setRating(5);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(reviewRepository.existsByClientIdAndProfileId(200L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.addReview(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void shouldThrowWhenInvalidRating() {
        AddReviewRequest request = new AddReviewRequest();
        request.setClientId(200L);
        request.setRating(10);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.addReview(1L, request))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void shouldReplyToReviewSuccessfully() {
        ProfileReview review = new ProfileReview();
        review.setId(10L);
        review.setProfile(profile);

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        ReplyToReviewRequest request = new ReplyToReviewRequest();
        request.setReply("Thank you!");

        ReviewResponse response = service.replyToReview(10L, 100L, request);

        assertThat(response).isNotNull();
        verify(reviewRepository).save(review);
    }

    @Test
    void shouldThrowWhenUnauthorizedReply() {
        ProfileReview review = new ProfileReview();
        review.setProfile(profile);

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        ReplyToReviewRequest request = new ReplyToReviewRequest();
        request.setReply("Reply");

        assertThatThrownBy(() ->
                service.replyToReview(10L, 999L, request)
        ).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void shouldHideReview() {
        ProfileReview review = new ProfileReview();
        review.setId(10L);

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        service.hideReview(10L);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        verify(reviewRepository).save(review);
    }

    @Test
    void shouldThrowWhenDeleteNotFound() {
        when(reviewRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteReview(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldRestoreReview() {
        ProfileReview review = new ProfileReview();
        review.setId(10L);
        review.setStatus(ReviewStatus.HIDDEN);

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenReturn(review);

        ReviewResponse response = service.restoreReview(10L);

        assertThat(response).isNotNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.VISIBLE);
    }
}