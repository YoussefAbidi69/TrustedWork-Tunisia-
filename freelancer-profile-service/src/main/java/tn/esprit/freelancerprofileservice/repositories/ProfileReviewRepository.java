package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.freelancerprofileservice.entities.ProfileReview;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;

import java.util.List;

public interface ProfileReviewRepository extends JpaRepository<ProfileReview, Long> {

    /**
     * Liste des reviews visibles d'un profil, triées de la plus récente à la plus ancienne
     */
    List<ProfileReview> findByProfileIdAndStatusOrderByReviewedAtDesc(Long profileId, ReviewStatus status);

    /**
     * Vérification anti-doublon :
     * un seul avis par client pour un même profil
     */
    boolean existsByClientIdAndProfileId(Long clientId, Long profileId);

    /**
     * Note moyenne d'un profil sur les avis visibles
     */
    @Query("""
           SELECT AVG(r.rating)
           FROM ProfileReview r
           WHERE r.profile.id = :profileId
             AND r.status = 'VISIBLE'
           """)
    Double findAverageRatingByProfileId(Long profileId);

    /**
     * Nombre total d'avis visibles pour un profil
     */
    long countByProfileIdAndStatus(Long profileId, ReviewStatus status);

    /**
     * Nombre d'avis visibles pour une note donnée (1 à 5)
     */
    long countByProfileIdAndRatingAndStatus(Long profileId, Integer rating, ReviewStatus status);

    long countByProfileId(Long profileId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM ProfileReview r WHERE r.profile.id = :profileId")
    double avgRatingByProfileId(@Param("profileId") Long profileId);
}