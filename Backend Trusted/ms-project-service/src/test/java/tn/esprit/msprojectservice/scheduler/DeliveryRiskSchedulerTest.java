package tn.esprit.msprojectservice.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.MLPredictionDTO;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.repositories.*;
import tn.esprit.msprojectservice.services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryRiskScheduler — Tests unitaires")
class DeliveryRiskSchedulerTest {

    @Mock private IProjectRepository     projectRepository;
    @Mock private ITaskRepository        taskRepository;
    @Mock private IRiskSignalRepository  riskSignalRepository;
    @Mock private IDeliverableRepository deliverableRepository;
    @Mock private IProgressReportService progressReportService;
    @Mock private INotificationService   notificationService;
    @Mock private IMLPredictionService   mlPredictionService;
    @Mock private IMailService           mailService;

    @InjectMocks
    private DeliveryRiskScheduler scheduler;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("Projet Test")
                .status(ProjectStatus.ACTIVE)
                .clientId(10L)
                .freelancerId(20L)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── analyzeAllActiveProjects ─────────────────────────────────────────────

    @Test
    @DisplayName("analyzeAllActiveProjects — analyse chaque projet actif")
    void analyzeAllActiveProjects_withActiveProjects_analysesEach() {
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(buildPrediction(null));
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        scheduler.analyzeAllActiveProjects();

        verify(mlPredictionService).predictDeliveryRisk(1L);
        verify(taskRepository).findBlockedTasksByProjectId(1L);
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — aucun projet actif → aucune analyse")
    void analyzeAllActiveProjects_noActiveProjects_noAnalysis() {
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of());

        scheduler.analyzeAllActiveProjects();

        verifyNoInteractions(mlPredictionService);
        verifyNoInteractions(taskRepository);
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — DELAY_RISK détecté → signal créé")
    void analyzeAllActiveProjects_delayRiskDetected_signalCreated() {
        MLPredictionDTO criticalPrediction = buildPrediction(RiskSeverity.CRITICAL);
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(criticalPrediction);
        when(riskSignalRepository.existsActiveProjectSignal(1L, RiskType.DELAY_RISK)).thenReturn(false);
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        scheduler.analyzeAllActiveProjects();

        verify(riskSignalRepository).save(any(DeliveryRiskSignal.class));
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — signal DELAY_RISK déjà actif → pas de doublon")
    void analyzeAllActiveProjects_existingDelayRisk_noNewSignal() {
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(buildPrediction(RiskSeverity.HIGH));
        when(riskSignalRepository.existsActiveProjectSignal(1L, RiskType.DELAY_RISK)).thenReturn(true);
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        scheduler.analyzeAllActiveProjects();

        verify(riskSignalRepository, never()).save(any(DeliveryRiskSignal.class));
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — BOTTLENECK détecté sur tâche bloquée")
    void analyzeAllActiveProjects_bottleneckDetected_signalCreated() {
        Task blockedTask = Task.builder()
                .id(5L).title("Tâche bloquée").status(TaskStatus.IN_PROGRESS)
                .updatedAt(LocalDateTime.now().minusDays(5)) // 5 jours bloquée >= seuil 3
                .build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(buildPrediction(null));
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of(blockedTask));
        when(riskSignalRepository.existsActiveSignal(1L, RiskType.BOTTLENECK, 5L)).thenReturn(false);
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(blockedTask));

        scheduler.analyzeAllActiveProjects();

        verify(riskSignalRepository).save(argThat(signal ->
                signal.getRiskType() == RiskType.BOTTLENECK));
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — INACTIVITY détecté après 5 jours sans activité")
    void analyzeAllActiveProjects_inactivityDetected_signalCreated() {
        Task inactiveTask = Task.builder()
                .id(3L).title("Tâche inactive").status(TaskStatus.TODO)
                .updatedAt(LocalDateTime.now().minusDays(6)) // 6 jours inactif >= seuil 5
                .build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(buildPrediction(null));
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(inactiveTask));
        when(riskSignalRepository.existsActiveProjectSignal(1L, RiskType.INACTIVITY)).thenReturn(false);

        scheduler.analyzeAllActiveProjects();

        verify(riskSignalRepository).save(argThat(signal ->
                signal.getRiskType() == RiskType.INACTIVITY));
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — SCOPE_CREEP détecté quand ratio > 0.30")
    void analyzeAllActiveProjects_scopeCreepDetected_signalCreated() {
        // 1 initial task + 2 added after start = ratio 2.0 > 0.30
        LocalDate startDate = sampleProject.getStartDate();
        Task initialTask = Task.builder().id(1L).title("Initial")
                .createdAt(startDate.minusDays(1).atStartOfDay()).updatedAt(LocalDateTime.now()).build();
        Task addedTask1 = Task.builder().id(2L).title("Added 1")
                .createdAt(startDate.plusDays(5).atStartOfDay()).updatedAt(LocalDateTime.now()).build();
        Task addedTask2 = Task.builder().id(3L).title("Added 2")
                .createdAt(startDate.plusDays(8).atStartOfDay()).updatedAt(LocalDateTime.now()).build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(buildPrediction(null));
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(initialTask, addedTask1, addedTask2));
        // INACTIVITY not triggered because tasks are recent (updatedAt=now)
        when(riskSignalRepository.existsActiveProjectSignal(1L, RiskType.SCOPE_CREEP)).thenReturn(false);

        scheduler.analyzeAllActiveProjects();

        verify(riskSignalRepository).save(argThat(signal ->
                signal.getRiskType() == RiskType.SCOPE_CREEP));
    }

    @Test
    @DisplayName("analyzeAllActiveProjects — exception sur un projet → traitement continue pour les autres")
    void analyzeAllActiveProjects_exceptionOnOneProject_continuesWithOthers() {
        Project project2 = Project.builder().id(2L).title("Projet 2")
                .clientId(11L).startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject, project2));
        when(mlPredictionService.predictDeliveryRisk(1L))
                .thenThrow(new RuntimeException("Erreur ML simulée"));
        when(mlPredictionService.predictDeliveryRisk(2L)).thenReturn(buildPrediction(null));
        when(taskRepository.findBlockedTasksByProjectId(2L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> scheduler.analyzeAllActiveProjects());
        verify(mlPredictionService).predictDeliveryRisk(2L);
    }

    // ─── generateWeeklyReports ────────────────────────────────────────────────

    @Test
    @DisplayName("generateWeeklyReports — génère et envoie rapport pour chaque projet actif")
    void generateWeeklyReports_activeProjects_generatesAndSendsReports() {
        ProgressReportDTO report = buildReport();
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(progressReportService.generateReport(1L)).thenReturn(report);

        scheduler.generateWeeklyReports();

        verify(progressReportService).generateReport(1L);
        verify(mailService).envoyerRapportHebdomadaire(sampleProject, report);
    }

    @Test
    @DisplayName("generateWeeklyReports — aucun projet actif → aucun rapport généré")
    void generateWeeklyReports_noActiveProjects_noReportsGenerated() {
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of());

        scheduler.generateWeeklyReports();

        verifyNoInteractions(progressReportService);
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("generateWeeklyReports — exception mail → traitement continue")
    void generateWeeklyReports_mailException_continuesProcessing() {
        ProgressReportDTO report = buildReport();
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(progressReportService.generateReport(1L)).thenReturn(report);
        doThrow(new RuntimeException("SMTP erreur")).when(mailService)
                .envoyerRapportHebdomadaire(any(), any());

        assertThatNoException().isThrownBy(() -> scheduler.generateWeeklyReports());
    }

    // ─── sendDailyNotifications ───────────────────────────────────────────────

    @Test
    @DisplayName("sendDailyNotifications — deadline imminente → notification DEADLINE_24H")
    void sendDailyNotifications_imminentDeadline_sendsDeadlineNotification() {
        Task taskDueToday = Task.builder()
                .id(1L).title("Tâche urgente").status(TaskStatus.IN_PROGRESS)
                .deadline(LocalDate.now()) // deadline aujourd'hui
                .assigneeId(30L)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(taskDueToday));
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());

        scheduler.sendDailyNotifications();

        verify(notificationService, atLeastOnce()).createNotification(
                anyLong(), anyString(), anyString(), eq(NotificationType.DEADLINE_24H), anyLong(), anyLong());
    }

    @Test
    @DisplayName("sendDailyNotifications — livrable en attente → notification DELIVERABLE_PENDING")
    void sendDailyNotifications_pendingDeliverable_sendsNotification() {
        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(3);
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());

        scheduler.sendDailyNotifications();

        verify(notificationService).createNotification(
                eq(10L), anyString(), anyString(), eq(NotificationType.DELIVERABLE_PENDING),
                eq(1L), isNull());
    }

    @Test
    @DisplayName("sendDailyNotifications — tâche bloquée >= 3j → notification TASK_BLOCKED")
    void sendDailyNotifications_blockedTask_sendsNotification() {
        Task blockedTask = Task.builder()
                .id(5L).title("Bloquée").status(TaskStatus.IN_REVIEW)
                .assigneeId(30L)
                .updatedAt(LocalDateTime.now().minusDays(4)) // bloquée 4 jours >= 3
                .build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(0);
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of(blockedTask));

        scheduler.sendDailyNotifications();

        verify(notificationService).createNotification(
                eq(30L), anyString(), anyString(), eq(NotificationType.TASK_BLOCKED),
                eq(1L), eq(5L));
    }

    @Test
    @DisplayName("sendDailyNotifications — tâche DONE sans deadline → pas de notification deadline")
    void sendDailyNotifications_doneTaskIgnored_noDeadlineNotification() {
        Task doneTask = Task.builder()
                .id(1L).title("Terminée").status(TaskStatus.DONE)
                .deadline(LocalDate.now())
                .build();

        when(projectRepository.findAllActiveProjects()).thenReturn(List.of(sampleProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(doneTask));
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(0);
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());

        scheduler.sendDailyNotifications();

        verify(notificationService, never()).createNotification(
                anyLong(), anyString(), anyString(), eq(NotificationType.DEADLINE_24H),
                anyLong(), anyLong());
    }

    // ─── analyzeProjectById ───────────────────────────────────────────────────

    @Test
    @DisplayName("analyzeProjectById — analyse le projet existant")
    void analyzeProjectById_existingProject_analyzesSuccessfully() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(mlPredictionService.predictDeliveryRisk(1L)).thenReturn(buildPrediction(null));
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> scheduler.analyzeProjectById(1L));

        verify(mlPredictionService).predictDeliveryRisk(1L);
    }

    @Test
    @DisplayName("analyzeProjectById — projet introuvable → EntityNotFoundException")
    void analyzeProjectById_projectNotFound_throwsEntityNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduler.analyzeProjectById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private MLPredictionDTO buildPrediction(RiskSeverity severity) {
        return MLPredictionDTO.builder()
                .probabilityLate(severity == null ? 0.2 : 0.9)
                .willBeLate(severity != null)
                .severity(severity)
                .message(severity == null ? "✅ Pas de risque" : "🚨 Risque critique")
                .criticalFeature("active_risks_count")
                .daysElapsedRatio(0.5)
                .tasksDoneRatio(0.5)
                .activeRisksCount(0)
                .bottleneckDaysAvg(0.0)
                .openDeliverablesCount(0)
                .assigneePerfScore(0.65)
                .scopeCreepRatio(0.0)
                .budgetConsumptionRatio(0.5)
                .build();
    }

    private ProgressReportDTO buildReport() {
        return ProgressReportDTO.builder()
                .id(1L).projectId(1L)
                .completionRate(60).totalTasks(10).completedTasks(6)
                .activeRisks(1).openDeliverables(0)
                .summary("Bon avancement")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
