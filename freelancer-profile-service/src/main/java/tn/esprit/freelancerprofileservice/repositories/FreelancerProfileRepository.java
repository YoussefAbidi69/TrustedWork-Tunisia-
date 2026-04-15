package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;

import java.util.List;
import java.util.Optional;

public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Long> {

    // Trouver le profil par userId (Module 01)
    Optional<FreelancerProfile> findByUserId(Long userId);

    // Freelancers disponibles par région
    List<FreelancerProfile> findByRegionAndAvailabilityStatus(String region, AvailabilityStatus status);

    // Classement régional trié par score
    List<FreelancerProfile> findByRegionOrderByCompletenessScoreDesc(String region);

    // Tous les profils publics
    @Query("SELECT f FROM FreelancerProfile f WHERE f.visibility = 'PUBLIC' ORDER BY f.completenessScore DESC")
    List<FreelancerProfile> findAllPublicProfiles();

    // Profils avec score inférieur à un seuil (pour rappels scheduler)
    @Query("SELECT f FROM FreelancerProfile f WHERE f.completenessScore < :threshold")
    List<FreelancerProfile> findProfilesBelowScore(@Param("threshold") Integer threshold);

    // Recherche multicritères
    @Query("""
        SELECT p FROM FreelancerProfile p
        WHERE p.visibility = 'PUBLIC'
          AND (:region IS NULL OR p.region = :region)
          AND (:availability IS NULL OR p.availabilityStatus = :availability)
          AND (:minRate IS NULL OR p.hourlyRate >= :minRate)
          AND (:maxRate IS NULL OR p.hourlyRate <= :maxRate)
        ORDER BY p.completenessScore DESC
    """)
    List<FreelancerProfile> searchProfiles(
            @Param("region") String region,
            @Param("availability") AvailabilityStatus availability,
            @Param("minRate") Double minRate,
            @Param("maxRate") Double maxRate
    );

    boolean existsByUserId(Long userId);
}