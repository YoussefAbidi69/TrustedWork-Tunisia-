package tn.esprit.freelancerprofileservice.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.clients.UserClient;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.SchedulerConfig;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;
import tn.esprit.freelancerprofileservice.services.ICompletenessService;
import tn.esprit.freelancerprofileservice.services.IEmailService;
import tn.esprit.freelancerprofileservice.services.ISchedulerConfigService;
import tn.esprit.freelancerprofileservice.services.ISkillAuthenticityService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileSchedulerTest {

    @Mock private FreelancerProfileRepository  profileRepository;
    @Mock private SkillRepository              skillRepository;
    @Mock private CertificationRepository      certificationRepository;
    @Mock private ISkillAuthenticityService    skillAuthenticityService;
    @Mock private ICompletenessService         completenessService;
    @Mock private IEmailService                emailService;
    @Mock private UserClient                   userClient;
    @Mock private ISchedulerConfigService      schedulerConfigService;

    @InjectMocks
    private ProfileScheduler scheduler;

    private FreelancerProfile profile;

    @BeforeEach
    void setUp() {
        profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setUserId(10L);
        profile.setRegion("Tunis");
        profile.setCompletenessScore(45);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // META-SCHEDULER : checkAndRunScheduledJobs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void checkAndRunScheduledJobs_shouldDoNothing_whenNoActiveConfigs() {
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of());

        scheduler.checkAndRunScheduledJobs();

        verify(profileRepository, never()).findAll();
    }

    @Test
    void checkAndRunScheduledJobs_shouldDoNothing_whenAllDisabled() {
        SchedulerConfig disabled = buildConfig("recalculateAllSkillScores", false, null, 1440);
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of(disabled));

        scheduler.checkAndRunScheduledJobs();

        verify(profileRepository, never()).findAll();
    }

    @Test
    void checkAndRunScheduledJobs_shouldNotRun_whenLastRunRecentAndIntervalNotElapsed() {
        // lastRun il y a 10 minutes, intervalle = 1440 min → pas encore dû
        SchedulerConfig config = buildConfig("recalculateAllSkillScores", true,
                LocalDateTime.now().minusMinutes(10), 1440);
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of(config));

        scheduler.checkAndRunScheduledJobs();

        verify(profileRepository, never()).findAll();
    }

    @Test
    void checkAndRunScheduledJobs_shouldRun_whenLastRunNullAndEnabled() {
        // lastRun = null → isDue retourne true → job se déclenche
        SchedulerConfig config = buildConfig("recalculateAllSkillScores", true, null, 1440);
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of(config));
        when(profileRepository.findAll()).thenReturn(List.of(profile));

        scheduler.checkAndRunScheduledJobs();

        verify(profileRepository).findAll();
        verify(schedulerConfigService).markLastRun("recalculateAllSkillScores");
    }

    @Test
    void checkAndRunScheduledJobs_shouldRun_whenIntervalElapsed() {
        // lastRun il y a 1500 minutes, intervalle = 1440 → dû
        SchedulerConfig config = buildConfig("recalculateAllSkillScores", true,
                LocalDateTime.now().minusMinutes(1500), 1440);
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of(config));
        when(profileRepository.findAll()).thenReturn(List.of(profile));

        scheduler.checkAndRunScheduledJobs();

        verify(profileRepository).findAll();
        verify(schedulerConfigService).markLastRun("recalculateAllSkillScores");
    }

    @Test
    void checkAndRunScheduledJobs_shouldIgnoreUnknownJobName() {
        SchedulerConfig config = buildConfig("jobInconnu", true, null, 1);
        when(schedulerConfigService.getAllConfigs()).thenReturn(List.of(config));

        // Ne doit pas lever d'exception, le job inconnu est juste loggé et ignoré
        scheduler.checkAndRunScheduledJobs();

        verify(schedulerConfigService).markLastRun("jobInconnu");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // triggerJobManually
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void triggerJobManually_shouldCallExecuteAndMarkLastRun() {
        when(profileRepository.findAll()).thenReturn(List.of(profile));

        scheduler.triggerJobManually("recalculateAllSkillScores");

        verify(schedulerConfigService).markLastRun("recalculateAllSkillScores");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÂCHE 1 : recalculateAllSkillScores
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void recalculateAllSkillScores_shouldCallRecalculateForEachProfile() {
        FreelancerProfile p2 = new FreelancerProfile();
        p2.setId(2L);
        when(profileRepository.findAll()).thenReturn(List.of(profile, p2));

        scheduler.recalculateAllSkillScores();

        verify(skillAuthenticityService).recalculateAllScores(1L);
        verify(skillAuthenticityService).recalculateAllScores(2L);
    }

    @Test
    void recalculateAllSkillScores_shouldNotThrow_whenProfileFails() {
        when(profileRepository.findAll()).thenReturn(List.of(profile));
        doThrow(new RuntimeException("Service down"))
                .when(skillAuthenticityService).recalculateAllScores(1L);

        // Ne doit pas propager l'exception
        scheduler.recalculateAllSkillScores();

        verify(skillAuthenticityService).recalculateAllScores(1L);
    }

    @Test
    void recalculateAllSkillScores_shouldDoNothing_whenNoProfiles() {
        when(profileRepository.findAll()).thenReturn(List.of());

        scheduler.recalculateAllSkillScores();

        verify(skillAuthenticityService, never()).recalculateAllScores(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÂCHE 2 : updateRegionalRankings
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void updateRegionalRankings_shouldRankProfilesByRegion() {
        FreelancerProfile p2 = new FreelancerProfile();
        p2.setId(2L);
        p2.setRegion("Tunis");

        when(profileRepository.findAll()).thenReturn(List.of(profile, p2));
        when(profileRepository.findByRegionOrderByCompletenessScoreDesc("Tunis"))
                .thenReturn(List.of(profile, p2));

        scheduler.updateRegionalRankings();

        verify(profileRepository).saveAll(anyList());
        // Le premier profil doit avoir le rang 1
        assert profile.getRegionalRank() == 1;
        assert p2.getRegionalRank() == 2;
    }

    @Test
    void updateRegionalRankings_shouldSkipProfilesWithNullRegion() {
        FreelancerProfile noRegion = new FreelancerProfile();
        noRegion.setId(3L);
        noRegion.setRegion(null);

        when(profileRepository.findAll()).thenReturn(List.of(noRegion));

        scheduler.updateRegionalRankings();

        verify(profileRepository, never()).findByRegionOrderByCompletenessScoreDesc(any());
        verify(profileRepository, never()).saveAll(any());
    }

    @Test
    void updateRegionalRankings_shouldSkipProfilesWithBlankRegion() {
        FreelancerProfile blankRegion = new FreelancerProfile();
        blankRegion.setId(4L);
        blankRegion.setRegion("   ");

        when(profileRepository.findAll()).thenReturn(List.of(blankRegion));

        scheduler.updateRegionalRankings();

        verify(profileRepository, never()).saveAll(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÂCHE 3 : sendProfileCompletionReminders
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void sendProfileCompletionReminders_shouldSendEmail_whenProfileIncomplete() {
        when(profileRepository.findProfilesBelowScore(60)).thenReturn(List.of(profile));
        when(userClient.getUserEmail(10L)).thenReturn("user@mail.com");
        when(userClient.getUserFullName(10L)).thenReturn("Test User");

        scheduler.sendProfileCompletionReminders();

        verify(completenessService).calculateCompleteness(10L);
        verify(emailService).sendProfileIncompleteReminder("user@mail.com", "Test User", 45);
    }

    @Test
    void sendProfileCompletionReminders_shouldSkip_whenEmailIsBlank() {
        when(profileRepository.findProfilesBelowScore(60)).thenReturn(List.of(profile));
        when(userClient.getUserEmail(10L)).thenReturn("");
        when(userClient.getUserFullName(10L)).thenReturn("Test User");

        scheduler.sendProfileCompletionReminders();

        verify(emailService, never()).sendProfileIncompleteReminder(any(), any(), anyInt());
    }

    @Test
    void sendProfileCompletionReminders_shouldSkip_whenEmailIsNull() {
        when(profileRepository.findProfilesBelowScore(60)).thenReturn(List.of(profile));
        when(userClient.getUserEmail(10L)).thenReturn(null);
        when(userClient.getUserFullName(10L)).thenReturn("Test User");

        scheduler.sendProfileCompletionReminders();

        verify(emailService, never()).sendProfileIncompleteReminder(any(), any(), anyInt());
    }

    @Test
    void sendProfileCompletionReminders_shouldNotThrow_whenProfileFails() {
        when(profileRepository.findProfilesBelowScore(60)).thenReturn(List.of(profile));
        doThrow(new RuntimeException("feign error")).when(userClient).getUserEmail(10L);

        scheduler.sendProfileCompletionReminders();

        verify(emailService, never()).sendProfileIncompleteReminder(any(), any(), anyInt());
    }

    @Test
    void sendProfileCompletionReminders_shouldDoNothing_whenNoIncompleteProfiles() {
        when(profileRepository.findProfilesBelowScore(60)).thenReturn(List.of());

        scheduler.sendProfileCompletionReminders();

        verify(emailService, never()).sendProfileIncompleteReminder(any(), any(), anyInt());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÂCHE 4 : checkCertificationExpiry
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void checkCertificationExpiry_shouldMarkExpiredAndSave() {
        Certification cert = buildCertification(LocalDate.now().minusDays(1)); // déjà expirée

        when(certificationRepository.findExpiringCertifications(any())).thenReturn(List.of(cert));

        scheduler.checkCertificationExpiry();

        verify(certificationRepository).save(cert);
        assert Boolean.TRUE.equals(cert.getIsExpired());
        // Completeness recalculé pour le profil impacté
        verify(completenessService).calculateCompleteness(10L);
    }

    @Test
    void checkCertificationExpiry_shouldSendAlertEmail_whenExpiringWithin30Days() {
        // Certification expire dans 10 jours (pas encore expirée)
        Certification cert = buildCertification(LocalDate.now().plusDays(10));

        when(certificationRepository.findExpiringCertifications(any())).thenReturn(List.of(cert));
        when(userClient.getUserEmail(10L)).thenReturn("user@mail.com");
        when(userClient.getUserFullName(10L)).thenReturn("Test User");

        scheduler.checkCertificationExpiry();

        verify(emailService).sendCertificationExpiryAlert(
                eq("user@mail.com"), eq("Test User"), eq("AWS SAA"), any());
        verify(certificationRepository, never()).save(cert);
    }

    @Test
    void checkCertificationExpiry_shouldSkipAlert_whenEmailNull() {
        Certification cert = buildCertification(LocalDate.now().plusDays(10));

        when(certificationRepository.findExpiringCertifications(any())).thenReturn(List.of(cert));
        when(userClient.getUserEmail(10L)).thenReturn(null);
        when(userClient.getUserFullName(10L)).thenReturn("Test User");

        scheduler.checkCertificationExpiry();

        verify(emailService, never()).sendCertificationExpiryAlert(any(), any(), any(), any());
    }

    @Test
    void checkCertificationExpiry_shouldDoNothing_whenNoCertificationsExpiring() {
        when(certificationRepository.findExpiringCertifications(any())).thenReturn(List.of());

        scheduler.checkCertificationExpiry();

        verify(certificationRepository, never()).save(any());
        verify(emailService, never()).sendCertificationExpiryAlert(any(), any(), any(), any());
    }

    @Test
    void checkCertificationExpiry_shouldNotThrow_whenCertFails() {
        Certification cert = buildCertification(LocalDate.now().minusDays(1));
        doThrow(new RuntimeException("DB error")).when(certificationRepository).save(cert);

        when(certificationRepository.findExpiringCertifications(any())).thenReturn(List.of(cert));

        // Ne doit pas propager l'exception
        scheduler.checkCertificationExpiry();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private SchedulerConfig buildConfig(String jobName, boolean enabled,
                                        LocalDateTime lastRun, int intervalMinutes) {
        return SchedulerConfig.builder()
                .id(1L)
                .jobName(jobName)
                .description("desc")
                .cronExpression("0 0 1 * * *")
                .intervalMinutes(intervalMinutes)
                .enabled(enabled)
                .lastRun(lastRun)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Certification buildCertification(LocalDate expiryDate) {
        Certification cert = new Certification();
        cert.setId(1L);
        cert.setTitle("AWS SAA");
        cert.setExpiryDate(expiryDate);
        cert.setIsExpired(false);
        cert.setProfile(profile);
        return cert;
    }
}
