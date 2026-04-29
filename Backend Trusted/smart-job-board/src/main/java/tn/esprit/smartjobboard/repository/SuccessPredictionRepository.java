package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.SuccessPrediction;

import java.util.Optional;

public interface SuccessPredictionRepository extends JpaRepository<SuccessPrediction, Long> {

    Optional<SuccessPrediction> findByJobOfferIdAndFreelancerId(Long jobOfferId, Long freelancerId);

    @Modifying
    @Query("DELETE FROM SuccessPrediction s WHERE s.jobOfferId = :jid")
    void deleteByJobOfferId(@Param("jid") Long jobOfferId);
}
