package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.ProfileReview;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReviewRepository;

import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;

import java.util.List;

/**
 * Implémentation du service des avis clients avec protection anti-spam
 */
@Service
@RequiredArgsConstructor
public class ProfileReviewServiceImpl implements IProfileReviewService {

    private final ProfileReviewRepository reviewRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public ProfileReview addReview(Long profileId, Long clientId, Integer rating, String comment) {
        if (reviewRepository.existsByClientIdAndProfileId(clientId, profileId)) {
            throw new DuplicateResourceException("Vous avez déjà laissé un avis sur ce profil");
        }
        if (rating < 1 || rating > 5) {
            throw new InvalidDataException("La note doit être entre 1 et 5");
        }
        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", profileId));

        ProfileReview review = ProfileReview.builder()
                .clientId(clientId)
                .profile(profile)
                .rating(rating)
                .comment(comment)
                .status(ReviewStatus.VISIBLE)
                .build();
        return reviewRepository.save(review);
    }

    @Override
    public List<ProfileReview> getVisibleReviews(Long profileId) {
        return reviewRepository.findByProfileIdAndStatus(profileId, ReviewStatus.VISIBLE);
    }

    @Override
    public Double getAverageRating(Long profileId) {
        Double avg = reviewRepository.findAverageRatingByProfileId(profileId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }
}