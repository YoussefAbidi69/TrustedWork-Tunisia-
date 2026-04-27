package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.response.ProfileReportResponse;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;

import java.util.List;

public interface IProfileReportService {

    ProfileReport reportProfile(Long profileId, Long reporterId, ReportCategory category, String description);

    List<ProfileReportResponse> getAllReports();

    List<ProfileReportResponse> getPendingReports();

    List<ProfileReportResponse> getReportsByStatus(ReportStatus status);

    List<ProfileReportResponse> getReportsByProfileId(Long profileId);

    ProfileReportResponse updateReportStatus(Long reportId, ReportStatus newStatus);
}