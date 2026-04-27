package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.JobDemandSnapshot;

public interface JobDemandSnapshotRepository extends JpaRepository<JobDemandSnapshot, Long> {

    @Modifying
    @Query("DELETE FROM JobDemandSnapshot j WHERE j.jobOfferId = :jobId")
    void deleteByJobOfferId(@Param("jobId") Long jobOfferId);
}
