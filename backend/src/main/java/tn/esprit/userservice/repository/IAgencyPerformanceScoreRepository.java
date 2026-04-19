package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.AgencyPerformanceScore;

import java.util.Optional;

public interface IAgencyPerformanceScoreRepository extends JpaRepository<AgencyPerformanceScore, Long> {

    Optional<AgencyPerformanceScore> findByAgencyId(Long agencyId);

    boolean existsByAgencyId(Long agencyId);
}