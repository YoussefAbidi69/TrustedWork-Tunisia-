package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReportRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implémentation du workflow de modération des signalements
 */
@Service
@RequiredArgsConstructor
public class ProfileReportServiceImpl implements IProfileReportService {

    private final ProfileReportRepository reportRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public ProfileReport reportProfile(Long profileId, Long reporterId, String reason) {
        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        ProfileReport report = ProfileReport.builder()
                .reporterId(reporterId)
                .profile(profile)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .build();
        return reportRepository.save(report);
    }

    @Override
    public List<ProfileReport> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    @Override
    public ProfileReport resolveReport(Long reportId, ReportStatus newStatus) {
        ProfileReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Signalement introuvable"));
        report.setStatus(newStatus);
        report.setResolvedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }
}