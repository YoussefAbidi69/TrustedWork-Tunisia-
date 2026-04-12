package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.ProfileReview;

import java.util.List;

public interface IProfileReviewService {
    ProfileReview addReview(Long profileId, Long clientId, Integer rating, String comment);
    List<ProfileReview> getVisibleReviews(Long profileId);
    Double getAverageRating(Long profileId);
}