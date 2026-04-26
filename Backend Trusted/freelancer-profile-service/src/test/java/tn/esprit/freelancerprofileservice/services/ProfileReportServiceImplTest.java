package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.clients.UserClient;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.ProfileReport;
import tn.esprit.freelancerprofileservice.enums.ReportCategory;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileReportServiceImplTest {

    @InjectMocks
    private ProfileReportServiceImpl service;

    @Mock private ProfileReportRepository reportRepository;
    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserClient userClient;
    @Mock private IEmailService emailService;
    @Mock private SmsService smsService;

    private FreelancerProfile profile;

    @BeforeEach
    void setup() {
        profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setUserId(10L);
        profile.setSuspended(false);
    }

    // =========================
    // reportProfile
    // =========================

    @Test
    void shouldCreateReportSuccessfully() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(reportRepository.existsByReporterIdAndProfileId(2L, 1L)).thenReturn(false);
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(reportRepository.countByProfileId(1L)).thenReturn(1L);

        ProfileReport result = service.reportProfile(
                1L, 2L, ReportCategory.SPAM, "Test description"
        );

        assertNotNull(result);
        assertEquals(ReportStatus.PENDING, result.getStatus());

        verify(reportRepository).save(any());
        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void shouldThrowWhenDuplicateReport() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(reportRepository.existsByReporterIdAndProfileId(2L, 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                service.reportProfile(1L, 2L, ReportCategory.SPAM, "desc")
        );
    }

    @Test
    void shouldThrowWhenProfileNotFound() {
        when(profileRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.reportProfile(1L, 2L, ReportCategory.SPAM, "desc")
        );
    }

    @Test
    void shouldThrowWhenInvalidData() {
        assertThrows(InvalidDataException.class, () ->
                service.reportProfile(null, 2L, ReportCategory.SPAM, "desc")
        );
    }

    // =========================
    // updateReportStatus
    // =========================

    @Test
    void shouldUpdateStatusAndSendNotifications() {
        ProfileReport report = new ProfileReport();
        report.setId(1L);
        report.setReporterId(2L);
        report.setProfile(profile);
        report.setDescription("test");

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenReturn(report);
        when(reportRepository.countByProfileId(1L)).thenReturn(1L);

        UserClient.PublicUserResponse user = new UserClient.PublicUserResponse();
        user.setEmail("test@mail.com");
        user.setFirstName("John");
        user.setLastName("Doe");

        when(userClient.getPublicUser(2L)).thenReturn(user);
        when(userClient.getUserFullName(any())).thenReturn("John Doe");

        var response = service.updateReportStatus(1L, ReportStatus.RESOLVED);

        assertNotNull(response);
        assertEquals(ReportStatus.RESOLVED, response.getStatus());

        verify(emailService).sendReportStatusEmail(any(), any(), any(), any());
        verify(smsService).sendReportResolvedSms(any(), any(), any());
        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void shouldThrowWhenReportNotFound() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.updateReportStatus(1L, ReportStatus.RESOLVED)
        );
    }

    // =========================
    // reportProfile — edge cases
    // =========================

    @Test
    void shouldThrowWhenNullReporterId() {
        assertThrows(InvalidDataException.class, () ->
                service.reportProfile(1L, null, ReportCategory.SPAM, "desc")
        );
    }

    @Test
    void shouldThrowWhenNullCategory() {
        // La validation de category=null se fait avant findById → pas de stub nécessaire
        assertThrows(InvalidDataException.class, () ->
                service.reportProfile(1L, 2L, null, "desc")
        );
    }

    @Test
    void shouldThrowWhenDescriptionEmpty() {
        // La validation de description vide se fait avant findById → pas de stub nécessaire
        assertThrows(InvalidDataException.class, () ->
                service.reportProfile(1L, 2L, ReportCategory.SPAM, "  ")
        );
    }

    @Test
    void shouldThrowWhenDescriptionTooLong() {
        // La validation de description trop longue se fait avant findById → pas de stub nécessaire
        String longDesc = "a".repeat(1001);
        assertThrows(InvalidDataException.class, () ->
                service.reportProfile(1L, 2L, ReportCategory.SPAM, longDesc)
        );
    }

    @Test
    void shouldThrowWhenSelfReport() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        // profile.getUserId() = 10L, reporterId also = 10L
        assertThrows(InvalidDataException.class, () ->
                service.reportProfile(1L, 10L, ReportCategory.SPAM, "self report")
        );
    }

    // =========================
    // updateReportStatus — statuses
    // =========================

    @Test
    void shouldUpdateStatus_IN_REVIEW_shouldNotSendSms() {
        ProfileReport report = new ProfileReport();
        report.setId(1L);
        report.setReporterId(2L);
        report.setProfile(profile);
        report.setDescription("test");

        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenReturn(report);
        when(reportRepository.countByProfileId(1L)).thenReturn(1L);

        UserClient.PublicUserResponse user = new UserClient.PublicUserResponse();
        user.setEmail("test@mail.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        when(userClient.getPublicUser(2L)).thenReturn(user);
        when(userClient.getUserFullName(any())).thenReturn("John Doe");

        var response = service.updateReportStatus(1L, ReportStatus.IN_REVIEW);

        assertNotNull(response);
        // SMS should NOT be sent for IN_REVIEW
        verify(smsService, never()).sendReportResolvedSms(any(), any(), any());
    }

    @Test
    void shouldThrowWhenNullReportId() {
        assertThrows(InvalidDataException.class, () ->
                service.updateReportStatus(null, ReportStatus.RESOLVED)
        );
    }

    @Test
    void shouldThrowWhenNullStatus() {
        assertThrows(InvalidDataException.class, () ->
                service.updateReportStatus(1L, null)
        );
    }

    // =========================
    // getAllReports
    // =========================

    @Test
    void getAllReports_shouldReturnMappedList() {
        ProfileReport report = new ProfileReport();
        report.setId(1L);
        report.setReporterId(2L);
        report.setProfile(profile);
        report.setDescription("spam");
        report.setCategory(ReportCategory.SPAM);
        report.setStatus(ReportStatus.PENDING);

        when(reportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(report));
        when(userClient.getUserFullName(any())).thenReturn("John Doe");

        var result = service.getAllReports();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reportRepository).findAllByOrderByCreatedAtDesc();
    }

    // =========================
    // getPendingReports
    // =========================

    @Test
    void getPendingReports_shouldReturnPendingOnly() {
        ProfileReport report = new ProfileReport();
        report.setId(2L);
        report.setReporterId(3L);
        report.setProfile(profile);
        report.setDescription("fake skills");
        report.setCategory(ReportCategory.FAKE_SKILLS);
        report.setStatus(ReportStatus.PENDING);

        when(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING))
                .thenReturn(List.of(report));
        when(userClient.getUserFullName(any())).thenReturn("Jane Doe");

        var result = service.getPendingReports();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // =========================
    // getReportsByStatus
    // =========================

    @Test
    void getReportsByStatus_shouldThrow_whenNullStatus() {
        assertThrows(InvalidDataException.class, () ->
                service.getReportsByStatus(null)
        );
    }

    @Test
    void getReportsByStatus_shouldReturnFiltered() {
        ProfileReport report = new ProfileReport();
        report.setId(3L);
        report.setReporterId(4L);
        report.setProfile(profile);
        report.setCategory(ReportCategory.SPAM);
        report.setStatus(ReportStatus.RESOLVED);

        when(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.RESOLVED))
                .thenReturn(List.of(report));
        when(userClient.getUserFullName(any())).thenReturn("Ahmed Ali");

        var result = service.getReportsByStatus(ReportStatus.RESOLVED);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // =========================
    // getReportsByProfileId
    // =========================

    @Test
    void getReportsByProfileId_shouldThrow_whenProfileNotFound() {
        when(profileRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                service.getReportsByProfileId(99L)
        );
    }

    @Test
    void getReportsByProfileId_shouldReturnList() {
        ProfileReport report = new ProfileReport();
        report.setId(4L);
        report.setReporterId(5L);
        report.setProfile(profile);
        report.setCategory(ReportCategory.SPAM);
        report.setStatus(ReportStatus.PENDING);

        when(profileRepository.existsById(1L)).thenReturn(true);
        when(reportRepository.findByProfileIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(report));
        when(userClient.getUserFullName(any())).thenReturn("Rania Jrad");

        var result = service.getReportsByProfileId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // =========================
    // Auto-suspension
    // =========================

    @Test
    void shouldSuspendProfile_whenThresholdReached() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(reportRepository.existsByReporterIdAndProfileId(2L, 1L)).thenReturn(false);
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // 5 reports triggers auto-suspension
        when(reportRepository.countByProfileId(1L)).thenReturn(5L);

        service.reportProfile(1L, 2L, ReportCategory.SPAM, "fifth report");

        assertTrue(profile.getSuspended());
        assertEquals(100, profile.getRiskScore());
        // Extra suspension notification persisted
        verify(notificationRepository, atLeast(2)).save(any());
    }
}