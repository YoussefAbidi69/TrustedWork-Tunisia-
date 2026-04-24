package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.msprojectservice.dto.MLPredictionDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.*;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MLPredictionServiceImpl — Tests unitaires")
class MLPredictionServiceImplTest {

    @Mock private IProjectRepository     projectRepository;
    @Mock private ITaskRepository        taskRepository;
    @Mock private IRiskSignalRepository  riskSignalRepository;
    @Mock private IDeliverableRepository deliverableRepository;

    @InjectMocks
    private MLPredictionServiceImpl mlService;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("Projet ML Test")
                .status(ProjectStatus.ACTIVE)
                .clientId(10L)
                .freelancerId(20L)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().plusDays(30))
                .budget(5000.0)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── predictDeliveryRisk — évaluateur null ────────────────────────────────

    @Test
    @DisplayName("predictDeliveryRisk — lève IllegalStateException si modèle non chargé")
    void predictDeliveryRisk_evaluatorNull_throwsIllegalStateException() {
        // evaluator is null by default (no loadModel() called via @InjectMocks)
        assertThatThrownBy(() -> mlService.predictDeliveryRisk(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("modèle ML");
    }

    // ─── loadModel — chargement réel du modèle PMML ───────────────────────────

    @Test
    @DisplayName("loadModel — charge le modèle PMML sans erreur")
    void loadModel_success_evaluatorIsNotNull() {
        mlService.loadModel();
        // If model loaded, predictDeliveryRisk no longer throws IllegalStateException for null evaluator
        // Instead it throws for project not found
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mlService.predictDeliveryRisk(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── determineSeverity — tous les seuils ─────────────────────────────────

    @Test
    @DisplayName("determineSeverity — probabilité >= 0.85 → CRITICAL")
    void determineSeverity_critical_returns_CRITICAL() throws Exception {
        assertThat(invokeDetermineSeverity(0.90)).isEqualTo(RiskSeverity.CRITICAL);
        assertThat(invokeDetermineSeverity(0.85)).isEqualTo(RiskSeverity.CRITICAL);
    }

    @Test
    @DisplayName("determineSeverity — probabilité [0.70, 0.85[ → HIGH")
    void determineSeverity_high_returns_HIGH() throws Exception {
        assertThat(invokeDetermineSeverity(0.70)).isEqualTo(RiskSeverity.HIGH);
        assertThat(invokeDetermineSeverity(0.75)).isEqualTo(RiskSeverity.HIGH);
    }

    @Test
    @DisplayName("determineSeverity — probabilité [0.50, 0.70[ → MEDIUM")
    void determineSeverity_medium_returns_MEDIUM() throws Exception {
        assertThat(invokeDetermineSeverity(0.50)).isEqualTo(RiskSeverity.MEDIUM);
        assertThat(invokeDetermineSeverity(0.65)).isEqualTo(RiskSeverity.MEDIUM);
    }

    @Test
    @DisplayName("determineSeverity — probabilité [0.35, 0.50[ → LOW")
    void determineSeverity_low_returns_LOW() throws Exception {
        assertThat(invokeDetermineSeverity(0.35)).isEqualTo(RiskSeverity.LOW);
        assertThat(invokeDetermineSeverity(0.40)).isEqualTo(RiskSeverity.LOW);
    }

    @Test
    @DisplayName("determineSeverity — probabilité < 0.35 → null (pas de risque)")
    void determineSeverity_noRisk_returnsNull() throws Exception {
        assertThat(invokeDetermineSeverity(0.20)).isNull();
        assertThat(invokeDetermineSeverity(0.0)).isNull();
        assertThat(invokeDetermineSeverity(0.34)).isNull();
    }

    // ─── buildMessage — tous les niveaux de sévérité ─────────────────────────

    @Test
    @DisplayName("buildMessage — severity null → message succès ✅")
    void buildMessage_nullSeverity_returnsSuccessMessage() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.20, null, "active_risks_count");
        assertThat(msg).contains("✅");
        assertThat(msg).contains(sampleProject.getTitle());
        assertThat(msg).contains("20%");
    }

    @Test
    @DisplayName("buildMessage — severity LOW → message vigilance 🟡")
    void buildMessage_lowSeverity_returnsLowRiskMessage() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.40, RiskSeverity.LOW, "active_risks_count");
        assertThat(msg).contains("🟡");
        assertThat(msg).contains(sampleProject.getTitle());
        assertThat(msg).contains("40%");
    }

    @Test
    @DisplayName("buildMessage — severity MEDIUM → message modéré 🟠")
    void buildMessage_mediumSeverity_returnsMediumRiskMessage() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.60, RiskSeverity.MEDIUM, "tasks_done_ratio");
        assertThat(msg).contains("🟠");
        assertThat(msg).contains("60%");
    }

    @Test
    @DisplayName("buildMessage — severity HIGH → message élevé 🔴")
    void buildMessage_highSeverity_returnsHighRiskMessage() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.75, RiskSeverity.HIGH, "bottleneck_days_avg");
        assertThat(msg).contains("🔴");
        assertThat(msg).contains("75%");
    }

    @Test
    @DisplayName("buildMessage — severity CRITICAL → message critique 🚨")
    void buildMessage_criticalSeverity_returnsCriticalMessage() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.92, RiskSeverity.CRITICAL, "scope_creep_ratio");
        assertThat(msg).contains("🚨");
        assertThat(msg).contains("92%");
    }

    @Test
    @DisplayName("buildMessage — feature inconnue → label brut utilisé")
    void buildMessage_unknownFeature_usesRawFeatureLabel() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.80, RiskSeverity.HIGH, "unknown_feature_xyz");
        assertThat(msg).contains("unknown_feature_xyz");
    }

    @Test
    @DisplayName("buildMessage — feature assignee_perf_score → label correct")
    void buildMessage_assigneePerfScoreFeature_usesCorrectLabel() throws Exception {
        String msg = invokeBuildMessage(sampleProject, 0.72, RiskSeverity.HIGH, "assignee_perf_score");
        assertThat(msg).contains("freelancer");
    }

    // ─── findCriticalFeature — identification du facteur critique ────────────

    @Test
    @DisplayName("findCriticalFeature — retourne l'une des features connues")
    void findCriticalFeature_returnsKnownFeatureName() throws Exception {
        String result = invokeFindCriticalFeature(0.8, 0.2, 5, 10.0, 0.5, 0.4);
        assertThat(result).isIn(
                "active_risks_count", "bottleneck_days_avg",
                "tasks_done_ratio", "assignee_perf_score", "scope_creep_ratio");
    }

    @Test
    @DisplayName("findCriticalFeature — nombreux risques actifs → active_risks_count prioritaire")
    void findCriticalFeature_highActiveRisks_returnsActiveRisksCount() throws Exception {
        // activeRisksCount=7 → riskNorm=1.0, score=0.36 (highest weight)
        String result = invokeFindCriticalFeature(0.5, 0.5, 7, 0.0, 0.9, 0.0);
        assertThat(result).isEqualTo("active_risks_count");
    }

    @Test
    @DisplayName("findCriticalFeature — bottleneck élevé → bottleneck_days_avg prioritaire")
    void findCriticalFeature_highBottleneck_returnsBottleneckDaysAvg() throws Exception {
        // bottleneckDaysAvg=15 → bottleneckN=1.0, score=0.16 ; but risks=0
        String result = invokeFindCriticalFeature(0.5, 0.5, 0, 15.0, 0.9, 0.0);
        assertThat(result).isEqualTo("bottleneck_days_avg");
    }

    // ─── predictDeliveryRisk — intégration avec modèle réel ─────────────────

    @Test
    @DisplayName("predictDeliveryRisk — retourne DTO complet avec modèle chargé")
    void predictDeliveryRisk_withLoadedModel_returnsCompleteDTO() {
        mlService.loadModel();

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(taskRepository.countTotalTasksByProjectId(1L)).thenReturn(10);
        when(taskRepository.countCompletedTasksByProjectId(1L)).thenReturn(5);
        when(riskSignalRepository.countActiveRisksByProjectId(1L)).thenReturn(2);
        when(taskRepository.findBlockedTasksByProjectId(1L)).thenReturn(List.of());
        when(deliverableRepository.countOpenDeliverablesByProjectId(1L)).thenReturn(1);
        when(taskRepository.findByAssigneeId(20L)).thenReturn(List.of());
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        MLPredictionDTO result = mlService.predictDeliveryRisk(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProbabilityLate()).isBetween(0.0, 1.0);
        assertThat(result.getDaysElapsedRatio()).isBetween(0.0, 1.0);
        assertThat(result.getTasksDoneRatio()).isBetween(0.0, 1.0);
        assertThat(result.getActiveRisksCount()).isEqualTo(2);
        assertThat(result.getOpenDeliverablesCount()).isEqualTo(1);
        assertThat(result.getCriticalFeature()).isNotNull();
        assertThat(result.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("predictDeliveryRisk — projet sans dates → daysElapsedRatio = 0.5")
    void predictDeliveryRisk_noProjectDates_defaultDaysRatio() {
        mlService.loadModel();

        Project noDateProject = Project.builder()
                .id(2L).title("Sans Dates")
                .freelancerId(null).budget(null)
                .startDate(null).endDate(null)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(2L)).thenReturn(Optional.of(noDateProject));
        when(taskRepository.countTotalTasksByProjectId(2L)).thenReturn(0);
        // countCompleted not called when total=0 (early return)
        when(riskSignalRepository.countActiveRisksByProjectId(2L)).thenReturn(0);
        when(taskRepository.findBlockedTasksByProjectId(2L)).thenReturn(List.of());
        when(deliverableRepository.countOpenDeliverablesByProjectId(2L)).thenReturn(0);
        // findByAssigneeId not called when freelancerId=null (early return → 0.65)
        // findByProjectId not called when startDate=null (early return → 0.0)

        MLPredictionDTO result = mlService.predictDeliveryRisk(2L);

        assertThat(result.getDaysElapsedRatio()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("predictDeliveryRisk — projet introuvable → RuntimeException")
    void predictDeliveryRisk_projectNotFound_throwsRuntimeException() {
        mlService.loadModel();
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mlService.predictDeliveryRisk(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─── calculateAssigneePerfScore — score de performance ────────────────────

    @Test
    @DisplayName("predictDeliveryRisk — freelancerId null → score neutre 0.65 utilisé")
    void predictDeliveryRisk_nullFreelancerId_usesNeutralPerfScore() {
        mlService.loadModel();

        Project projectNoFreelancer = Project.builder()
                .id(3L).title("No Freelancer")
                .freelancerId(null)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(10))
                .budget(1000.0)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .build();

        when(projectRepository.findById(3L)).thenReturn(Optional.of(projectNoFreelancer));
        when(taskRepository.countTotalTasksByProjectId(3L)).thenReturn(5);
        when(taskRepository.countCompletedTasksByProjectId(3L)).thenReturn(3);
        when(riskSignalRepository.countActiveRisksByProjectId(3L)).thenReturn(0);
        when(taskRepository.findBlockedTasksByProjectId(3L)).thenReturn(List.of());
        when(deliverableRepository.countOpenDeliverablesByProjectId(3L)).thenReturn(0);
        // findByAssigneeId not called when freelancerId=null (early return → 0.65)
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        MLPredictionDTO result = mlService.predictDeliveryRisk(3L);

        assertThat(result.getAssigneePerfScore()).isEqualTo(0.65);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private RiskSeverity invokeDetermineSeverity(double probability) throws Exception {
        Method m = MLPredictionServiceImpl.class.getDeclaredMethod("determineSeverity", double.class);
        m.setAccessible(true);
        return (RiskSeverity) m.invoke(mlService, probability);
    }

    @SuppressWarnings("unchecked")
    private String invokeBuildMessage(Project project, double prob, RiskSeverity severity,
                                      String criticalFeature) throws Exception {
        Method m = MLPredictionServiceImpl.class.getDeclaredMethod(
                "buildMessage", Project.class, double.class, RiskSeverity.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(mlService, project, prob, severity, criticalFeature);
    }

    private String invokeFindCriticalFeature(double daysElapsedRatio, double tasksDoneRatio,
                                             int activeRisksCount, double bottleneckDaysAvg,
                                             double assigneePerfScore, double scopeCreepRatio) throws Exception {
        Method m = MLPredictionServiceImpl.class.getDeclaredMethod(
                "findCriticalFeature",
                double.class, double.class, int.class, double.class, double.class, double.class);
        m.setAccessible(true);
        return (String) m.invoke(mlService, daysElapsedRatio, tasksDoneRatio,
                activeRisksCount, bottleneckDaysAvg, assigneePerfScore, scopeCreepRatio);
    }
}
