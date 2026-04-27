package tn.esprit.smartjobboard.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.entity.JobApplication;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    long countByJobOfferId(Long jobOfferId);

    boolean existsByJobOfferIdAndFreelancerId(Long jobOfferId, Long freelancerId);

    Optional<JobApplication> findByJobOfferIdAndFreelancerId(Long jobOfferId, Long freelancerId);

    @Query("SELECT DISTINCT a FROM JobApplication a JOIN FETCH a.jobOffer j WHERE j.id = :jid")
    List<JobApplication> findByJobOfferIdWithJob(@Param("jid") Long jobOfferId);

    @Modifying
    @Query("DELETE FROM JobApplication a WHERE a.jobOffer.id = :jid")
    void deleteByJobOfferId(@Param("jid") Long jobOfferId);

    @Query("SELECT DISTINCT a FROM JobApplication a JOIN FETCH a.jobOffer WHERE a.freelancerId = :fid")
    List<JobApplication> findMineWithJobOffer(@Param("fid") Long freelancerId);

    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);
}
