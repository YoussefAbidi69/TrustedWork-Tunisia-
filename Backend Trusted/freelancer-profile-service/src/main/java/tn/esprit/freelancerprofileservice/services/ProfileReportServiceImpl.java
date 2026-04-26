package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.clients.UserClient;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReportResponse;
import tn.esprit.freelancerprofileservice.dto.websocket.ReportNotificationMessage;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Notification;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.NotificationRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReportRepository;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implémentation du workflow de modération des signalements de profil.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProfileReportServiceImpl implements IProfileReportService {

    private static final long AUTO_SUSPENSION_THRESHOLD = 5;
    private static final String ADMIN_REPORTS_TOPIC = "/topic/admin/reports";
    private static final int RISK_SCORE_PER_REPORT = 20;
    private static final int MAX_RISK_SCORE = 100;
    private static final long ADMIN_USER_ID = 0L;

    private final ProfileReportRepository reportRepository;
    private final FreelancerProfileRepository profileRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserClient userClient;
    private final IEmailService emailService;
    private final SmsService smsService;


    @Override
    public ProfileReport reportProfile(Long profileId, Long reporterId,
                                       ReportCategory category, String description) {
        if (profileId == null) throw new InvalidDataException("Profile ID is required");
        if (reporterId == null) throw new InvalidDataException("Reporter ID is required");
        if (category == null) throw new InvalidDataException("Report category is required");
        if (description == null || description.trim().isEmpty()) {
            throw new InvalidDataException("Report description is required");
        }
        if (description.trim().length() > 1000) {
            throw new InvalidDataException("Report description must not exceed 1000 characters");
        }

        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Freelancer profile not found with ID: " + profileId));

        if (profile.getUserId() != null && profile.getUserId().equals(reporterId)) {
            throw new InvalidDataException("You cannot report your own profile");
        }

        if (reportRepository.existsByReporterIdAndProfileId(reporterId, profileId)) {
            throw new DuplicateResourceException("You have already reported this profile");
        }

        ProfileReport report = ProfileReport.builder()
                .reporterId(reporterId)
                .profile(profile)
                .category(category)
                .description(description.trim())
                .status(ReportStatus.PENDING)
                .build();

        ProfileReport savedReport = reportRepository.save(report);

        persisterNotificationAdmin(
                savedReport,
                "NEW_REPORT",
                "Nouveau signalement — catégorie : " + category.name()
                        + " | profil #" + profileId
        );

        sendNewReportNotification(savedReport);
        refreshProfileModerationState(profile);

        return savedReport;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileReportResponse> getPendingReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileReportResponse> getReportsByStatus(ReportStatus status) {
        if (status == null) throw new InvalidDataException("Report status is required");

        return reportRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileReportResponse> getReportsByProfileId(Long profileId) {
        if (profileId == null) throw new InvalidDataException("Profile ID is required");
        if (!profileRepository.existsById(profileId)) {
            throw new ResourceNotFoundException("Freelancer profile not found with ID: " + profileId);
        }

        return reportRepository.findByProfileIdOrderByCreatedAtDesc(profileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ProfileReportResponse updateReportStatus(Long reportId, ReportStatus newStatus) {
        if (reportId == null) throw new InvalidDataException("Report ID is required");
        if (newStatus == null) throw new InvalidDataException("New report status is required");

        ProfileReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile report not found with ID: " + reportId));

        report.setStatus(newStatus);
        report.setResolvedAt(
                (newStatus == ReportStatus.RESOLVED || newStatus == ReportStatus.REJECTED)
                        ? LocalDateTime.now()
                        : null
        );

        ProfileReport updatedReport = reportRepository.save(report);

        persisterNotificationAdmin(
                updatedReport,
                "REPORT_STATUS_UPDATED",
                "Signalement #" + reportId + " → " + newStatus.name()
                        + " | profil #" + updatedReport.getProfile().getId()
        );

        sendStatusEmailToReporter(updatedReport);
        // Envoi SMS si rapport résolu ou rejeté
        if (newStatus == ReportStatus.RESOLVED || newStatus == ReportStatus.REJECTED) {
            sendReportResolvedSms(updatedReport);
        }

        refreshProfileModerationState(updatedReport.getProfile());
        sendReportStatusUpdatedNotification(updatedReport);

        return mapToResponse(updatedReport);
    }

    // =========================================================
    // EMAIL
    // =========================================================

    private void sendStatusEmailToReporter(ProfileReport report) {
        log.debug(">>> MAIL STEP 1 - sendStatusEmailToReporter called for report #{}", report.getId());

        try {
            Long reporterId = report.getReporterId();
            log.debug(">>> MAIL STEP 2 - reporterId = {}", reporterId);

            if (reporterId == null) {
                log.warn(">>> MAIL STOP - reporterId is null");
                return;
            }

            UserClient.PublicUserResponse reporter = userClient.getPublicUser(reporterId);

            if (reporter == null) {
                log.warn(">>> MAIL STOP - reporter response is null");
                return;
            }

            String email = reporter.getEmail() != null ? reporter.getEmail().trim() : null;

            String firstName = reporter.getFirstName() != null ? reporter.getFirstName().trim() : "";
            String lastName = reporter.getLastName() != null ? reporter.getLastName().trim() : "";
            String fullName = (firstName + " " + lastName).trim();

            if (fullName.isBlank()) {
                fullName = "Utilisateur";
            }

            log.debug(">>> MAIL STEP 3 - email = {}", email);
            log.debug(">>> MAIL STEP 4 - fullName = {}", fullName);

            if (email == null || email.isBlank()) {
                log.warn(">>> MAIL STOP - email is null or blank");
                return;
            }

            emailService.sendReportStatusEmail(
                    email,
                    fullName,
                    report.getStatus().name(),
                    report.getDescription()
            );

            log.info(">>> MAIL SUCCESS - email request launched for {}", email);

        } catch (Exception e) {
            log.error(">>> MAIL ERROR - {} - {}", e.getClass().getName(), e.getMessage(), e);
            e.printStackTrace();
        }
    }

    // =========================================================
    // PERSISTANCE NOTIFICATIONS ADMIN
    // =========================================================

    private void persisterNotificationAdmin(ProfileReport report, String type, String message) {
        String payload = "{"
                + "\"reportId\":" + report.getId()
                + ",\"profileId\":" + report.getProfile().getId()
                + ",\"reporterId\":" + report.getReporterId()
                + ",\"category\":\"" + (report.getCategory() != null ? report.getCategory().name() : "") + "\""
                + ",\"status\":\"" + report.getStatus().name() + "\""
                + "}";

        Notification notification = Notification.builder()
                .userId(ADMIN_USER_ID)
                .type(type)
                .message(message)
                .payload(payload)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    // =========================================================
    // WEBSOCKET
    // =========================================================

    private void sendNewReportNotification(ProfileReport report) {
        ReportNotificationMessage message = ReportNotificationMessage.builder()
                .type("NEW_REPORT")
                .reportId(report.getId())
                .profileId(report.getProfile().getId())
                .reporterId(report.getReporterId())
                .category(report.getCategory().name())
                .status(report.getStatus().name())
                .message("New profile report submitted")
                .createdAt(report.getCreatedAt() != null
                        ? report.getCreatedAt().toString()
                        : LocalDateTime.now().toString())
                .build();

        messagingTemplate.convertAndSend(ADMIN_REPORTS_TOPIC, message);
    }

    private void sendReportStatusUpdatedNotification(ProfileReport report) {
        ReportNotificationMessage message = ReportNotificationMessage.builder()
                .type("REPORT_STATUS_UPDATED")
                .reportId(report.getId())
                .profileId(report.getProfile().getId())
                .reporterId(report.getReporterId())
                .category(report.getCategory() != null ? report.getCategory().name() : null)
                .status(report.getStatus().name())
                .message("Report status updated to " + report.getStatus().name())
                .createdAt(LocalDateTime.now().toString())
                .build();

        messagingTemplate.convertAndSend(ADMIN_REPORTS_TOPIC, message);
    }

    // =========================================================
    // MODÉRATION AUTOMATIQUE
    // =========================================================

    private void refreshProfileModerationState(FreelancerProfile profile) {
        long totalReports = reportRepository.countByProfileId(profile.getId());
        int riskScore = calculateRiskScore(totalReports);
        boolean shouldBeSuspended = totalReports >= AUTO_SUSPENSION_THRESHOLD;
        boolean wasSuspended = Boolean.TRUE.equals(profile.getSuspended());

        profile.setRiskScore(riskScore);
        profile.setSuspended(shouldBeSuspended);
        profileRepository.save(profile);

        if (!wasSuspended && shouldBeSuspended) {
            persisterNotificationAdmin(
                    ProfileReport.builder()
                            .id(0L)
                            .profile(profile)
                            .reporterId(0L)
                            .category(ReportCategory.FAKE_SKILLS)
                            .status(ReportStatus.PENDING)
                            .build(),
                    "PROFILE_SUSPENDED",
                    "Profil #" + profile.getId()
                            + " suspendu automatiquement ("
                            + totalReports + " signalements, risk score : " + riskScore + ")"
            );

            sendProfileSuspendedNotification(profile, totalReports, riskScore);
        }
    }

    private int calculateRiskScore(long totalReports) {
        return (int) Math.min(totalReports * RISK_SCORE_PER_REPORT, MAX_RISK_SCORE);
    }

    private void sendProfileSuspendedNotification(FreelancerProfile profile, long totalReports, int riskScore) {
        ReportNotificationMessage message = ReportNotificationMessage.builder()
                .type("PROFILE_SUSPENDED")
                .profileId(profile.getId())
                .message("Profile automatically suspended due to multiple reports ("
                        + totalReports + " reports, risk score: " + riskScore + ")")
                .createdAt(LocalDateTime.now().toString())
                .build();

        messagingTemplate.convertAndSend(ADMIN_REPORTS_TOPIC, message);
    }

    // =========================================================
    // MAPPING
    // =========================================================

    private ProfileReportResponse mapToResponse(ProfileReport report) {
        FreelancerProfile profile = report.getProfile();

        String reporterName = userClient.getUserFullName(report.getReporterId());
        String freelancerName = userClient.getUserFullName(profile.getUserId());

        return ProfileReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .reporterName(reporterName)
                .profileId(profile.getId())
                .freelancerUserId(profile.getUserId())
                .freelancerName(freelancerName)
                .category(report.getCategory())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedAt(report.getResolvedAt())
                .riskScore(profile.getRiskScore())
                .suspended(profile.getSuspended())
                .build();
    }

    // =========================================================
// SMS
// =========================================================

    private void sendReportResolvedSms(ProfileReport report) {
        try {
            Long freelancerUserId = report.getProfile().getUserId();
            // Récupérer le prénom pour personnaliser le SMS
            String firstName = userClient.getUserFullName(freelancerUserId).split(" ")[0];

            smsService.sendReportResolvedSms(
                    firstName,
                    report.getId(),
                    report.getStatus().name()
            );

            log.info(">>> SMS lancé pour rapport #{}", report.getId());

        } catch (Exception e) {
            log.error(">>> SMS ERROR — {}", e.getMessage());
        }
    }
}