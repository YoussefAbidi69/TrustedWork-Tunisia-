package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.util.List;

public interface ProfileReportRepository extends JpaRepository<ProfileReport, Long> {

    List<ProfileReport> findByProfileId(Long profileId);

    List<ProfileReport> findByStatus(ReportStatus status);

    long countByProfileIdAndStatus(Long profileId, ReportStatus status);
}