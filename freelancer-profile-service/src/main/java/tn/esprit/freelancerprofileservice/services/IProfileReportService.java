package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.util.List;

public interface IProfileReportService {
    ProfileReport reportProfile(Long profileId, Long reporterId, String reason);
    List<ProfileReport> getPendingReports();
    ProfileReport resolveReport(Long reportId, ReportStatus newStatus);
}