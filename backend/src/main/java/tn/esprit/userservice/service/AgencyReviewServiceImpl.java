package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.entity.AgencyReview;
import tn.esprit.userservice.entity.ReviewTargetType;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.IAgencyReviewRepository;
import tn.esprit.userservice.service.IAgencyReviewServices;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyReviewServiceImpl implements IAgencyReviewServices {

    private final IAgencyReviewRepository agencyReviewRepository;
    private final IAgencyRepository agencyRepository;

    @Override
    public AgencyReview createReview(Long agencyId, AgencyReview review) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        review.setAgency(agency);
        review.setReviewedAt(LocalDateTime.now());

        if (review.getTargetType() == null) {
            review.setTargetType(ReviewTargetType.AGENCY);
        }

        return agencyReviewRepository.save(review);
    }

    @Override
    public List<AgencyReview> getReviewsByAgency(Long agencyId) {
        return agencyReviewRepository.findByAgencyId(agencyId);
    }

    @Override
    public List<AgencyReview> getReviewsByAgencyAndTargetType(Long agencyId, ReviewTargetType targetType) {
        return agencyReviewRepository.findByAgencyIdAndTargetType(agencyId, targetType);
    }

    @Override
    public List<AgencyReview> getReviewsByProject(Long projectId) {
        return agencyReviewRepository.findByProjectId(projectId);
    }

    @Override
    public double getAverageRatingByAgency(Long agencyId) {
        List<AgencyReview> reviews = agencyReviewRepository.findByAgencyId(agencyId);

        if (reviews.isEmpty()) {
            return 0.0;
        }

        return reviews.stream()
                .mapToInt(AgencyReview::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public AgencyReview getReviewById(Long reviewId) {
        return agencyReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    @Override
    public void deleteReview(Long reviewId) {
        agencyReviewRepository.deleteById(reviewId);
    }
}