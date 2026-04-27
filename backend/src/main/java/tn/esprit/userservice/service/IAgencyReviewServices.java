package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.AgencyReview;
import tn.esprit.userservice.entity.ReviewTargetType;

import java.util.List;

public interface IAgencyReviewServices {

    AgencyReview createReview(Long agencyId, AgencyReview review);

    List<AgencyReview> getReviewsByAgency(Long agencyId);

    List<AgencyReview> getReviewsByAgencyAndTargetType(Long agencyId, ReviewTargetType targetType);

    List<AgencyReview> getReviewsByProject(Long projectId);

    double getAverageRatingByAgency(Long agencyId);

    AgencyReview getReviewById(Long reviewId);

    void deleteReview(Long reviewId);
}
