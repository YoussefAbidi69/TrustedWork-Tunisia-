package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.OpportunityNotificationLog;

public interface OpportunityNotificationLogRepository extends JpaRepository<OpportunityNotificationLog, Long> {

    boolean existsByJobOfferIdAndFreelancerId(Long jobOfferId, Long freelancerId);

    @Modifying
    @Query("DELETE FROM OpportunityNotificationLog n WHERE n.jobOfferId = :jid")
    void deleteByJobOfferId(@Param("jid") Long jobOfferId);
}
