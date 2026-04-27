package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.BudgetIntelligence;

import java.util.Optional;

public interface BudgetIntelligenceRepository extends JpaRepository<BudgetIntelligence, Long> {

    Optional<BudgetIntelligence> findByJobOfferId(Long jobOfferId);

    @Modifying
    @Query("DELETE FROM BudgetIntelligence b WHERE b.jobOfferId = :jid")
    void deleteByJobOfferId(@Param("jid") Long jobOfferId);
}
