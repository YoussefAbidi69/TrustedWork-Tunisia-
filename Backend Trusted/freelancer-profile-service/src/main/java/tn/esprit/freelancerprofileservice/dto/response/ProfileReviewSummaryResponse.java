package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * DTO de synthèse des avis d'un profil freelancer
 */
@Data
@Builder
public class ProfileReviewSummaryResponse {

    private Long profileId;

    private Double averageRating;

    private Long totalReviews;

    private Long fiveStarCount;

    private Long fourStarCount;

    private Long threeStarCount;

    private Long twoStarCount;

    private Long oneStarCount;
}