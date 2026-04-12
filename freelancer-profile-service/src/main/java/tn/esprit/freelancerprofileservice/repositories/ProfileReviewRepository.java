package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.freelancerprofileservice.entities.ProfileReview;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;

import java.util.List;

public interface ProfileReviewRepository extends JpaRepository<ProfileReview, Long> {

    List<ProfileReview> findByProfileIdAndStatus(Long profileId, ReviewStatus status);

    // Vérification anti-spam : un seul avis par (client, profil)
    boolean existsByClientIdAndProfileId(Long clientId, Long profileId);

    // Note moyenne d'un profil
    @Query("SELECT AVG(r.rating) FROM ProfileReview r WHERE r.profile.id = :profileId AND r.status = 'VISIBLE'")
    Double findAverageRatingByProfileId(Long profileId);

    long countByProfileId(Long profileId);
}