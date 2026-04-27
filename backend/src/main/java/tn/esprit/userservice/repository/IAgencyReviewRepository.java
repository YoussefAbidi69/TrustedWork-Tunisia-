package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.AgencyReview;
import tn.esprit.userservice.entity.ReviewTargetType;

import java.util.List;

public interface IAgencyReviewRepository extends JpaRepository<AgencyReview, Long> {

    List<AgencyReview> findByAgencyId(Long agencyId);

    List<AgencyReview> findByAgencyIdAndTargetType(Long agencyId, ReviewTargetType targetType);

    List<AgencyReview> findByProjectId(Long projectId);

    List<AgencyReview> findByReviewerUserId(Long reviewerUserId);
}