package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.MatchScore;

import java.util.Optional;

public interface MatchScoreRepository extends JpaRepository<MatchScore, Long> {

    Optional<MatchScore> findByJobOfferIdAndFreelancerId(Long jobOfferId, Long freelancerId);

    @Modifying
    @Query("DELETE FROM MatchScore m WHERE m.jobOfferId = :jobId")
    void deleteByJobOfferId(@Param("jobId") Long jobOfferId);

    @Query("SELECT AVG(m.totalScore) FROM MatchScore m WHERE m.totalScore > 0")
    Double averageTotalScore();
}
