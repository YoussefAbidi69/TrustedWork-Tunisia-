package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProgressReportServiceImpl — Tests unitaires")
class ProgressReportServiceImplTest {

    @Mock
    private IProgressReportRepository progressReportRepository;

    @Mock
    private IProjectRepository projectRepository;

    @Mock
    private ITaskRepository taskRepository;

    @Mock
    private IDeliverableRepository deliverableRepository;

    @Mock
    private IRiskSignalRepository riskSignalRepository;

    @InjectMocks
    private ProgressReportServiceImpl progressReportService;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("Projet Intelligence Artificielle")
                .status(ProjectStatus.ACTIVE)
                .completionRate(60)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .budget(25000.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // generateReport
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateReport — calcule les métriques et sauvegarde le rapport")
    void generateReport_shouldCalculateMetricsAndSave() {
        ProgressReport savedReport = buildReport(1L, 6, 10, 60, 2, 1,
                "Rapport de progression — Projet Intelligence Artificielle.");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(10);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(6);
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(2);
        when(riskSignalRepository.countActiveRisksByProjectId(1L)).thenReturn(1);
        when(progressReportRepository.save(any(ProgressReport.class))).thenReturn(savedReport);

        ProgressReportDTO result = progressReportService.generateReport(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCompletedTasks()).isEqualTo(6);
        assertThat(result.getTotalTasks()).isEqualTo(10);
        assertThat(result.getCompletionRate()).isEqualTo(60);
        assertThat(result.getOpenDeliverables()).isEqualTo(2);
        assertThat(result.getActiveRisks()).isEqualTo(1);
        assertThat(result.getSummary()).isNotBlank();
        verify(progressReportRepository).save(any(ProgressReport.class));
    }

    @Test
    @DisplayName("generateReport — completionRate = 0 quand aucune tâche")
    void generateReport_shouldSetZeroRate_whenNoTasks() {
        ProgressReport emptyReport = buildReport(2L, 0, 0, 0, 0, 0, "...");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(0);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(0);
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(0);
        when(riskSignalRepository.countActiveRisksByProjectId(1L)).thenReturn(0);
        when(progressReportRepository.save(any(ProgressReport.class))).thenReturn(emptyReport);

        ProgressReportDTO result = progressReportService.generateReport(1L);

        assertThat(result.getCompletionRate()).isZero();
    }

    @Test
    @DisplayName("generateReport — résumé contient '100%' et '✅' quand projet terminé")
    void generateReport_shouldContainCompletionMessage_whenFinished() {
        ProgressReport fullReport = buildReport(3L, 5, 5, 100, 0, 0, "");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(5);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(5);
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(0);
        when(riskSignalRepository.countActiveRisksByProjectId(1L)).thenReturn(0);
        when(progressReportRepository.save(any(ProgressReport.class))).thenAnswer(inv -> {
            ProgressReport r = inv.getArgument(0);
            assertThat(r.getSummary()).contains("✅").contains("terminé");
            return fullReport;
        });

        progressReportService.generateReport(1L);
    }

    @Test
    @DisplayName("generateReport — résumé mentionne les risques actifs")
    void generateReport_shouldMentionRisks_whenActiveRisksExist() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(10);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(3);
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(1);
        when(riskSignalRepository.countActiveRisksByProjectId(1L)).thenReturn(3);
        when(progressReportRepository.save(any(ProgressReport.class))).thenAnswer(inv -> {
            ProgressReport r = inv.getArgument(0);
            assertThat(r.getSummary()).contains("⚠").contains("3");
            return buildReport(4L, 3, 10, 30, 1, 3, r.getSummary());
        });

        progressReportService.generateReport(1L);
    }

    @Test
    @DisplayName("generateReport — résumé indique 'Tous les livrables ont été traités' quand aucun livrable ouvert")
    void generateReport_shouldIndicateAllDeliverablesDone_whenZeroOpen() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(4);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(3);
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(0);
        when(riskSignalRepository.countActiveRisksByProjectId(1L)).thenReturn(0);
        when(progressReportRepository.save(any(ProgressReport.class))).thenAnswer(inv -> {
            ProgressReport r = inv.getArgument(0);
            assertThat(r.getSummary()).contains("Tous les livrables ont été traités");
            return buildReport(5L, 3, 4, 75, 0, 0, r.getSummary());
        });

        progressReportService.generateReport(1L);
    }

    @Test
    @DisplayName("generateReport — lève RuntimeException si projet introuvable")
    void generateReport_shouldThrow_whenProjectNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressReportService.generateReport(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(progressReportRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getLatestReport
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLatestReport — retourne le rapport le plus récent")
    void getLatestReport_shouldReturnLatest_whenExists() {
        ProgressReport report = buildReport(1L, 8, 10, 80, 0, 2, "Résumé");
        when(progressReportRepository.findLatestByProjectId(1L)).thenReturn(Optional.of(report));

        ProgressReportDTO result = progressReportService.getLatestReport(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCompletionRate()).isEqualTo(80);
    }

    @Test
    @DisplayName("getLatestReport — lève RuntimeException si aucun rapport")
    void getLatestReport_shouldThrow_whenNoReport() {
        when(progressReportRepository.findLatestByProjectId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressReportService.getLatestReport(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getReportHistory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getReportHistory — retourne les rapports dans l'ordre décroissant")
    void getReportHistory_shouldReturnOrderedList() {
        ProgressReport r1 = buildReport(1L, 3, 10, 30, 1, 0, "Résumé 1");
        ProgressReport r2 = buildReport(2L, 7, 10, 70, 0, 1, "Résumé 2");

        when(progressReportRepository.findByProjectIdOrderByGeneratedAtDesc(1L))
                .thenReturn(List.of(r2, r1));

        List<ProgressReportDTO> results = progressReportService.getReportHistory(1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(2L);
        assertThat(results.get(1).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getReportHistory — retourne liste vide si aucun historique")
    void getReportHistory_shouldReturnEmptyList_whenNone() {
        when(progressReportRepository.findByProjectIdOrderByGeneratedAtDesc(1L))
                .thenReturn(List.of());

        assertThat(progressReportService.getReportHistory(1L)).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ProgressReport buildReport(Long id, int completed, int total, int rate,
                                       int openDel, int activeRisks, String summary) {
        return ProgressReport.builder()
                .id(id)
                .project(sampleProject)
                .completedTasks(completed)
                .totalTasks(total)
                .completionRate(rate)
                .openDeliverables(openDel)
                .activeRisks(activeRisks)
                .summary(summary)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
