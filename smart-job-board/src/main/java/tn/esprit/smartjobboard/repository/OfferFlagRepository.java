package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.OfferFlag;

import java.util.List;

public interface OfferFlagRepository extends JpaRepository<OfferFlag, Long> {

    List<OfferFlag> findByJobOffer_IdOrderByIdAsc(Long jobOfferId);

    @Modifying
    @Query("DELETE FROM OfferFlag o WHERE o.jobOffer.id = :jobId")
    void deleteByJobOfferId(@Param("jobId") Long jobOfferId);
}
