package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.entity.CompatibilityReport;

import java.util.Optional;

public interface CompatibilityReportRepository extends JpaRepository<CompatibilityReport, Long> {

    @Query("SELECT c FROM CompatibilityReport c WHERE c.matchScoreId = :matchScoreId")
    Optional<CompatibilityReport> findByMatchScoreId(@Param("matchScoreId") Long matchScoreId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM CompatibilityReport c WHERE c.jobOfferId = :jobId")
    int purgeByJobOfferId(@Param("jobId") Long jobOfferId);
}