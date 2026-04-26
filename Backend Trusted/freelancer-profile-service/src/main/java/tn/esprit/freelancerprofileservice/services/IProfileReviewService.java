package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.request.AddReviewRequest;
import tn.esprit.freelancerprofileservice.dto.request.ReplyToReviewRequest;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReviewSummaryResponse;
import tn.esprit.freelancerprofileservice.dto.response.ReviewResponse;

import java.util.List;

public interface IProfileReviewService {

    ReviewResponse addReview(Long profileId, AddReviewRequest request);

    List<ReviewResponse> getVisibleReviews(Long profileId);

    /** Tous les avis du profil (VISIBLE + HIDDEN + FLAGGED) — réservé au propriétaire */
    List<ReviewResponse> getAllReviews(Long profileId);

    Double getAverageRating(Long profileId);

    ProfileReviewSummaryResponse getReviewSummary(Long profileId);

    ReviewResponse replyToReview(Long reviewId, Long freelancerUserId, ReplyToReviewRequest request);

    void hideReview(Long reviewId);

    void deleteReview(Long reviewId);

    public ReviewResponse restoreReview(Long reviewId);
}