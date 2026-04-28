package tn.esprit.userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.userservice.entity.FreelancerRecommendationScore;

import java.util.Optional;

@Repository
public interface FreelancerRecommendationScoreRepository extends JpaRepository<FreelancerRecommendationScore, Long> {
    
    Page<FreelancerRecommendationScore> findByAgencyIdOrderByRecommendationScoreDesc(Long agencyId, Pageable pageable);
    
    Optional<FreelancerRecommendationScore> findTopByAgencyIdOrderByComputedAtDesc(Long agencyId);
    
    void deleteByAgencyIdAndFreelancerId(Long agencyId, Long freelancerId);
    
    Optional<FreelancerRecommendationScore> findByAgencyIdAndFreelancerId(Long agencyId, Long freelancerId);
    
    void deleteByAgencyId(Long agencyId);
}
