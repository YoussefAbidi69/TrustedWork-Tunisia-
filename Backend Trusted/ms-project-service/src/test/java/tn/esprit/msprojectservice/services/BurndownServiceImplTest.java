package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.BurndownChartDTO;
import tn.esprit.msprojectservice.dto.BurndownPointDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BurndownServiceImpl — Tests unitaires")
class BurndownServiceImplTest {

    @Mock private IProjectRepository projectRepository;
    @Mock private ITaskRepository    taskRepository;
    @InjectMocks private BurndownServiceImpl burndownService;

    private Project baseProject;

    @BeforeEach
    void setUp() {
        baseProject = buildProject(LocalDate.now().minusDays(10), LocalDate.now().plusDays(30));
    }

    // ─── getBurndownChart — exceptions ───────────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — lève EntityNotFoundException si projet introuvable")
    void getBurndownChart_projectNotFound_throwsEntityNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> burndownService.getBurndownChart(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(taskRepository, never()).findByProjectId(any());
    }

    @Test
    @DisplayName("getBurndownChart — lève IllegalStateException si startDate null")
    void getBurndownChart_nullStartDate_throwsIllegalStateException() {
        Project p = Project.builder().id(2L).title("NoStart").startDate(null)
                .endDate(LocalDate.now().plusDays(10)).build();
        when(projectRepository.findById(2L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> burndownService.getBurndownChart(2L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("getBurndownChart — lève IllegalStateException si endDate null")
    void getBurndownChart_nullEndDate_throwsIllegalStateException() {
        Project p = Project.builder().id(3L).title("NoEnd")
                .startDate(LocalDate.now().minusDays(5)).endDate(null).build();
        when(projectRepository.findById(3L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> burndownService.getBurndownChart(3L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─── getBurndownChart — projet sans tâches (buildEmptyBurndown) ──────────

    @Test
    @DisplayName("getBurndownChart — retourne burndown vide si aucune tâche")
    void getBurndownChart_noTasks_returnsEmptyBurndown() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(baseProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getTotalTaches()).isZero();
        assertThat(result.getCourbeIdeale()).isEmpty();
        assertThat(result.getCourbeReelle()).isEmpty();
        assertThat(result.getTachesRestantesAujourdhui()).isZero();
        assertThat(result.getTachesCompletees()).isZero();
        assertThat(result.getRetardEstimeJours()).isZero();
        assertThat(result.getVelociteJournaliere()).isZero();
        assertThat(result.getStatutBurndown()).isEqualTo("DANS_LES_DELAIS");
        assertThat(result.getAnalyse()).contains("Aucune tâche");
    }

    // ─── getBurndownChart — toutes tâches terminées (statut DANS_LES_DELAIS) ─

    @Test
    @DisplayName("getBurndownChart — toutes tâches DONE → retard=0, analyse terminé")
    void getBurndownChart_allTasksDone_zeroRemainingAndCompletedAnalysis() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(baseProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildDoneTask(1L, LocalDateTime.now().minusDays(3)),
                buildDoneTask(2L, LocalDateTime.now().minusDays(2))
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getTachesRestantesAujourdhui()).isZero();
        assertThat(result.getTachesCompletees()).isEqualTo(2);
        assertThat(result.getStatutBurndown()).isEqualTo("DANS_LES_DELAIS");
        assertThat(result.getAnalyse()).contains("✅");
        assertThat(result.getAnalyse()).contains("terminé");
        assertThat(result.getCourbeIdeale()).isNotEmpty();
        assertThat(result.getCourbeReelle()).isNotEmpty();
    }

    // ─── statut EN_AVANCE (retard < -3) ─────────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — statut EN_AVANCE et analyse 🟢")
    void getBurndownChart_projectAhead_statusEnAvance() {
        // startDate=10d ago, endDate=100d future : velocite=8/10=0.8, dateProjete=now+3, retard=-97
        Project project = buildProject(LocalDate.now().minusDays(10), LocalDate.now().plusDays(100));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            tasks.add(buildDoneTask((long) i, LocalDateTime.now().minusDays(2)));
        }
        tasks.add(buildPendingTask(9L));
        tasks.add(buildPendingTask(10L));
        when(taskRepository.findByProjectId(1L)).thenReturn(tasks);

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getStatutBurndown()).isEqualTo("EN_AVANCE");
        assertThat(result.getAnalyse()).contains("🟢");
        assertThat(result.getRetardEstimeJours()).isLessThan(-3);
        assertThat(result.getCourbeIdeale()).isNotEmpty();
        assertThat(result.getCourbeReelle()).isNotEmpty();
        assertThat(result.getTotalTaches()).isEqualTo(10);
        assertThat(result.getTachesCompletees()).isEqualTo(8);
        assertThat(result.getTachesRestantesAujourdhui()).isEqualTo(2);
        assertThat(result.getVelociteJournaliere()).isGreaterThan(0);
    }

    // ─── statut DANS_LES_DELAIS (retard = 0) ─────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — statut DANS_LES_DELAIS et analyse 🟡")
    void getBurndownChart_projectOnSchedule_statusDansLesDelais() {
        // startDate=10d ago, endDate=2d future : velocite=10/10=1, joursNec=2, retard=0
        Project project = buildProject(LocalDate.now().minusDays(10), LocalDate.now().plusDays(2));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tasks.add(buildDoneTask((long) i, LocalDateTime.now().minusDays(1)));
        }
        tasks.add(buildPendingTask(11L));
        tasks.add(buildPendingTask(12L));
        when(taskRepository.findByProjectId(1L)).thenReturn(tasks);

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getStatutBurndown()).isEqualTo("DANS_LES_DELAIS");
        assertThat(result.getAnalyse()).contains("🟡");
        assertThat(result.getRetardEstimeJours()).isZero();
    }

    // ─── statut EN_RETARD (retard 1–5) ───────────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — statut EN_RETARD et analyse 🟠")
    void getBurndownChart_projectLate_statusEnRetard() {
        // startDate=5d ago, endDate=4d future : velocite=5/5=1, joursNec=5, retard=1
        Project project = buildProject(LocalDate.now().minusDays(5), LocalDate.now().plusDays(4));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            tasks.add(buildDoneTask((long) i, LocalDateTime.now().minusDays(1)));
        }
        for (int i = 6; i <= 10; i++) {
            tasks.add(buildPendingTask((long) i));
        }
        when(taskRepository.findByProjectId(1L)).thenReturn(tasks);

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getStatutBurndown()).isEqualTo("EN_RETARD");
        assertThat(result.getAnalyse()).contains("🟠");
        assertThat(result.getRetardEstimeJours()).isBetween(1, 5);
    }

    // ─── statut CRITIQUE (retard > 5) ────────────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — statut CRITIQUE et analyse 🔴")
    void getBurndownChart_projectCritical_statusCritique() {
        // startDate=5d ago, endDate=1d future : velocite=5/5=1, joursNec=10, retard=9
        Project project = buildProject(LocalDate.now().minusDays(5), LocalDate.now().plusDays(1));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            tasks.add(buildDoneTask((long) i, LocalDateTime.now().minusDays(1)));
        }
        for (int i = 6; i <= 15; i++) {
            tasks.add(buildPendingTask((long) i));
        }
        when(taskRepository.findByProjectId(1L)).thenReturn(tasks);

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getStatutBurndown()).isEqualTo("CRITIQUE");
        assertThat(result.getAnalyse()).contains("🔴");
        assertThat(result.getRetardEstimeJours()).isGreaterThan(5);
    }

    // ─── vélocité nulle (aucune tâche complétée) ─────────────────────────────

    @Test
    @DisplayName("getBurndownChart — vélocité 0 quand aucune tâche DONE")
    void getBurndownChart_noCompletedTasks_zeroVelocity() {
        // endDate=20d future, no DONE → velocite=0, retard négative (EN_AVANCE ou DANS_LES_DELAIS)
        Project project = buildProject(LocalDate.now().minusDays(10), LocalDate.now().plusDays(20));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildPendingTask(1L), buildPendingTask(2L), buildPendingTask(3L)
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getVelociteJournaliere()).isZero();
        assertThat(result.getTachesCompletees()).isZero();
        assertThat(result.getTachesRestantesAujourdhui()).isEqualTo(3);
        // velocite=0 → retard = -days(now, endDate) = -20 → EN_AVANCE
        assertThat(result.getStatutBurndown()).isEqualTo("EN_AVANCE");
    }

    // ─── endDate dans le passé (pas de projection future) ────────────────────

    @Test
    @DisplayName("getBurndownChart — endDate passé → pas de projection future, retard CRITIQUE")
    void getBurndownChart_endDateInPast_pastOnlyCurve() {
        Project project = buildProject(LocalDate.now().minusDays(20), LocalDate.now().minusDays(5));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildDoneTask(1L, LocalDateTime.now().minusDays(10)),
                buildPendingTask(2L)
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProjectId()).isEqualTo(1L);
        // Past project → high delay
        assertThat(result.getStatutBurndown()).isIn("EN_RETARD", "CRITIQUE");
        assertThat(result.getCourbeReelle()).isNotEmpty();
        // No future data points beyond endDate
        result.getCourbeReelle().forEach(p ->
                assertThat(p.getDate()).isBeforeOrEqualTo(LocalDate.now()));
    }

    // ─── startDate == endDate (durée nulle, edge case) ───────────────────────

    @Test
    @DisplayName("getBurndownChart — startDate == endDate → courbeIdeale 1 point, courbeReelle vide")
    void getBurndownChart_sameDates_singlePointIdealCurve() {
        LocalDate today = LocalDate.now();
        Project project = buildProject(today, today);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(buildPendingTask(1L)));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getCourbeIdeale()).hasSize(1);
        assertThat(result.getCourbeIdeale().get(0).getTachesRestantes()).isEqualTo(1);
        assertThat(result.getCourbeReelle()).isEmpty();
    }

    // ─── métadonnées du projet retournées correctement ───────────────────────

    @Test
    @DisplayName("getBurndownChart — retourne les métadonnées correctes du projet")
    void getBurndownChart_returnsCorrectProjectMetadata() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(baseProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildDoneTask(1L, LocalDateTime.now().minusDays(3)),
                buildPendingTask(2L)
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getProjectTitle()).isEqualTo("Projet Test");
        assertThat(result.getStartDate()).isEqualTo(baseProject.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(baseProject.getEndDate());
        assertThat(result.getTotalTaches()).isEqualTo(2);
        assertThat(result.getDateLivraisonProjetee()).isNotNull();
    }

    // ─── courbe idéale décroissante ───────────────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — courbe idéale décroît du total vers 0")
    void getBurndownChart_idealCurveIsMonotonicallyDecreasing() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(baseProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildPendingTask(1L), buildPendingTask(2L), buildPendingTask(3L)
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        List<BurndownPointDTO> courbe = result.getCourbeIdeale();
        assertThat(courbe).isNotEmpty();
        assertThat(courbe.get(0).getTachesRestantes()).isEqualTo(3);
        assertThat(courbe.get(courbe.size() - 1).getTachesRestantes()).isZero();
        for (int i = 1; i < courbe.size(); i++) {
            assertThat(courbe.get(i).getTachesRestantes())
                    .isLessThanOrEqualTo(courbe.get(i - 1).getTachesRestantes());
        }
    }

    // ─── courbe réelle contient un point par jour ─────────────────────────────

    @Test
    @DisplayName("getBurndownChart — courbe réelle inclut dates passées et projection future")
    void getBurndownChart_realCurveCoversHistoryAndFuture() {
        Project project = buildProject(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildDoneTask(1L, LocalDateTime.now().minusDays(2)),
                buildPendingTask(2L)
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getCourbeReelle()).isNotEmpty();
        // Courbe doit couvrir de startDate à endDate
        BurndownPointDTO firstPoint = result.getCourbeReelle().get(0);
        assertThat(firstPoint.getDate()).isEqualTo(project.getStartDate());
    }

    // ─── tâches complétées le même jour groupées correctement ─────────────────

    @Test
    @DisplayName("getBurndownChart — plusieurs tâches DONE le même jour correctement groupées")
    void getBurndownChart_multipleTasksDoneOnSameDay_groupedCorrectly() {
        Project project = buildProject(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                buildDoneTask(1L, yesterday),
                buildDoneTask(2L, yesterday),
                buildDoneTask(3L, yesterday),
                buildPendingTask(4L)
        ));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getTachesCompletees()).isEqualTo(3);
        assertThat(result.getTachesRestantesAujourdhui()).isEqualTo(1);
        assertThat(result.getCourbeReelle()).isNotEmpty();
    }

    // ─── un seul tâche dans le projet ─────────────────────────────────────────

    @Test
    @DisplayName("getBurndownChart — projet avec une seule tâche")
    void getBurndownChart_singleTask_worksFine() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(baseProject));
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(buildPendingTask(1L)));

        BurndownChartDTO result = burndownService.getBurndownChart(1L);

        assertThat(result.getTotalTaches()).isEqualTo(1);
        assertThat(result.getTachesRestantesAujourdhui()).isEqualTo(1);
        assertThat(result.getCourbeIdeale()).isNotEmpty();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Project buildProject(LocalDate start, LocalDate end) {
        return Project.builder()
                .id(1L)
                .title("Projet Test")
                .status(ProjectStatus.ACTIVE)
                .startDate(start)
                .endDate(end)
                .build();
    }

    private Task buildDoneTask(Long id, LocalDateTime updatedAt) {
        return Task.builder()
                .id(id)
                .title("Task DONE " + id)
                .status(TaskStatus.DONE)
                .project(baseProject)
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(updatedAt)
                .build();
    }

    private Task buildPendingTask(Long id) {
        return Task.builder()
                .id(id)
                .title("Task PENDING " + id)
                .status(TaskStatus.TODO)
                .project(baseProject)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
