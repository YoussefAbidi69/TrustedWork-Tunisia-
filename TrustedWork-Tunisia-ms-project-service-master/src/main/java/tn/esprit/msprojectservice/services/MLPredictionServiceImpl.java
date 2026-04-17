package tn.esprit.msprojectservice.services;

import jakarta.annotation.PostConstruct;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.*;
import org.jpmml.model.PMMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.MLPredictionDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
/**
 * Service ML — Prédiction du risque de retard via Random Forest (PMML).
 *
 * Fonctionnement :
 *   1. @PostConstruct charge le modèle delivery_risk_model.pmml au démarrage
 *   2. predictDeliveryRisk() calcule les 8 features depuis la DB
 *   3. Le modèle retourne P(retard) entre 0.0 et 1.0
 *   4. La sévérité est déduite automatiquement depuis la probabilité
 */
@Service
public class MLPredictionServiceImpl implements IMLPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(MLPredictionServiceImpl.class);

    // Chemin du modèle dans src/main/resources/
    private static final String MODEL_PATH = "/ml/delivery_risk_model.pmml";

    // Seuils de sévérité basés sur P(retard)
    private static final double THRESHOLD_CRITICAL = 0.85;
    private static final double THRESHOLD_HIGH     = 0.70;
    private static final double THRESHOLD_MEDIUM   = 0.50;
    private static final double THRESHOLD_LOW      = 0.35;

    @Autowired
    private IProjectRepository projectRepository;

    @Autowired
    private ITaskRepository taskRepository;

    @Autowired
    private IRiskSignalRepository riskSignalRepository;

    @Autowired
    private IDeliverableRepository deliverableRepository;

    // Évaluateur JPMML — chargé une seule fois au démarrage
    private Evaluator evaluator;

    // ============================================================
    // CHARGEMENT DU MODÈLE AU DÉMARRAGE
    // ============================================================

    @PostConstruct
    public void loadModel() {
        try {
            InputStream modelStream = getClass().getResourceAsStream(MODEL_PATH);

            if (modelStream == null) {
                throw new RuntimeException(
                        "Modèle PMML introuvable : " + MODEL_PATH +
                                " — Copier delivery_risk_model.pmml dans src/main/resources/ml/"
                );
            }

            // ✅ API correcte pour pmml-evaluator-metro 1.7.x
            this.evaluator = new LoadingModelEvaluatorBuilder()
                    .setLocatable(false)
                    .load(modelStream)
                    .build();

            this.evaluator.verify();

            logger.info("✅ Modèle Random Forest chargé avec succès — {}", MODEL_PATH);

        } catch (Exception e) {
            logger.error("❌ Erreur chargement modèle PMML : {}", e.getMessage());
        }
    }

    // ============================================================
    // PRÉDICTION PRINCIPALE
    // ============================================================

    @Override
    public MLPredictionDTO predictDeliveryRisk(Long projectId) {

        // Vérification que le modèle est chargé
        if (evaluator == null) {
            throw new RuntimeException(
                    "Le modèle ML n'est pas disponible. " +
                            "Vérifier que delivery_risk_model.pmml est dans src/main/resources/ml/"
            );
        }

        // Récupérer le projet
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projectId));

        // --------------------------------------------------------
        // CALCUL DES 8 FEATURES depuis la base de données
        // --------------------------------------------------------

        // Feature 1 : % temps écoulé / durée totale
        double daysElapsedRatio = calculateDaysElapsedRatio(project);

        // Feature 2 : % tâches DONE / total tâches
        double tasksDoneRatio = calculateTasksDoneRatio(projectId);

        // Feature 3 : Nombre de signaux de risque actifs
        int activeRisksCount = riskSignalRepository.countActiveRisksByProjectId(projectId);

        // Feature 4 : Moyenne jours de blocage (tâches IN_PROGRESS ou IN_REVIEW)
        double bottleneckDaysAvg = calculateBottleneckDaysAvg(projectId);

        // Feature 5 : Livrables en attente (SUBMITTED non validés)
        int openDeliverablesCount = deliverableRepository.countOpenDeliverablesByProjectId(projectId);

        // Feature 6 : Score de performance historique du freelancer
        double assigneePerfScore = calculateAssigneePerfScore(project.getFreelancerId());

        // Feature 7 : Ratio scope creep (tâches ajoutées après startDate)
        double scopeCreepRatio = calculateScopeCreepRatio(project);

        // Feature 8 : Ratio budget consommé / budget total
        double budgetConsumptionRatio = calculateBudgetConsumptionRatio(project);

        // --------------------------------------------------------
        // APPEL DU MODÈLE RANDOM FOREST
        // --------------------------------------------------------
        double probabilityLate = callModel(
                daysElapsedRatio, tasksDoneRatio, activeRisksCount,
                bottleneckDaysAvg, openDeliverablesCount, assigneePerfScore,
                scopeCreepRatio, budgetConsumptionRatio
        );

        // --------------------------------------------------------
        // DÉDUCTION DE LA SÉVÉRITÉ ET DU MESSAGE
        // --------------------------------------------------------
        boolean willBeLate   = probabilityLate >= THRESHOLD_MEDIUM;
        RiskSeverity severity = determineSeverity(probabilityLate);
        String criticalFeature = findCriticalFeature(
                daysElapsedRatio, tasksDoneRatio, activeRisksCount,
                bottleneckDaysAvg, assigneePerfScore, scopeCreepRatio
        );
        String message = buildMessage(project, probabilityLate, severity, criticalFeature);

        logger.info("🤖 ML Prédiction — Projet: {} | P(retard): {:.2f} | Sévérité: {}",
                project.getTitle(), probabilityLate, severity);

        return MLPredictionDTO.builder()
                .probabilityLate(Math.round(probabilityLate * 1000.0) / 1000.0)
                .willBeLate(willBeLate)
                .severity(severity)
                .message(message)
                .criticalFeature(criticalFeature)
                .daysElapsedRatio(daysElapsedRatio)
                .tasksDoneRatio(tasksDoneRatio)
                .activeRisksCount(activeRisksCount)
                .bottleneckDaysAvg(bottleneckDaysAvg)
                .openDeliverablesCount(openDeliverablesCount)
                .assigneePerfScore(assigneePerfScore)
                .scopeCreepRatio(scopeCreepRatio)
                .budgetConsumptionRatio(budgetConsumptionRatio)
                .build();
    }

    // ============================================================
    // APPEL JPMML
    // ============================================================

    private double callModel(
            double daysElapsedRatio, double tasksDoneRatio, int activeRisksCount,
            double bottleneckDaysAvg, int openDeliverablesCount, double assigneePerfScore,
            double scopeCreepRatio, double budgetConsumptionRatio) {

        // Construction du vecteur de features
        Map<String, Object> rawFeatures = new LinkedHashMap<>();
        rawFeatures.put("days_elapsed_ratio",        daysElapsedRatio);
        rawFeatures.put("tasks_done_ratio",           tasksDoneRatio);
        rawFeatures.put("active_risks_count",         (double) activeRisksCount);
        rawFeatures.put("bottleneck_days_avg",        bottleneckDaysAvg);
        rawFeatures.put("open_deliverables_count",    (double) openDeliverablesCount);
        rawFeatures.put("assignee_perf_score",        assigneePerfScore);
        rawFeatures.put("scope_creep_ratio",          scopeCreepRatio);
        rawFeatures.put("budget_consumption_ratio",   budgetConsumptionRatio);

        // Conversion en FieldValue JPMML
        Map<String, FieldValue> arguments = new LinkedHashMap<>();
        for (InputField inputField : evaluator.getInputFields()) {
            String name = inputField.getName();
            arguments.put(name, inputField.prepare(rawFeatures.get(name)));
        }

        // Inférence
        Map<String, ?> results = evaluator.evaluate(arguments);

        // Extraire P(will_be_late = 1) depuis les probabilités
        Object probabilities = results.get("probability(1)");
        if (probabilities instanceof Number) {
            return ((Number) probabilities).doubleValue();
        }

        // Fallback : extraire depuis la prédiction binaire
        Object prediction = results.get("will_be_late");
        return prediction != null && prediction.toString().equals("1") ? 0.75 : 0.25;
    }

    // ============================================================
    // CALCUL DES FEATURES
    // ============================================================

    private double calculateDaysElapsedRatio(Project project) {
        if (project.getStartDate() == null || project.getEndDate() == null) {
            return 0.5;
        }
        long totalDays   = ChronoUnit.DAYS.between(project.getStartDate(), project.getEndDate());
        long elapsedDays = ChronoUnit.DAYS.between(project.getStartDate(), LocalDate.now());
        if (totalDays <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, (double) elapsedDays / totalDays));
    }

    private double calculateTasksDoneRatio(Long projectId) {
        int total = taskRepository.countTotalTasksByProjectId(projectId);
        if (total == 0) return 0.0;
        int done = taskRepository.countCompletedTasksByProjectId(projectId);
        return (double) done / total;
    }

    private double calculateBottleneckDaysAvg(Long projectId) {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByProjectId(projectId);
        if (blockedTasks.isEmpty()) return 0.0;
        double totalDays = blockedTasks.stream()
                .mapToLong(t -> ChronoUnit.DAYS.between(t.getUpdatedAt().toLocalDate(), LocalDate.now()))
                .sum();
        return totalDays / blockedTasks.size();
    }

    private double calculateAssigneePerfScore(Long freelancerId) {
        if (freelancerId == null) return 0.65; // score neutre
        List<Task> tasks = taskRepository.findByAssigneeId(freelancerId);
        if (tasks.isEmpty()) return 0.65;
        long done = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getDeadline() == null ||
                        !t.getUpdatedAt().toLocalDate().isAfter(t.getDeadline()))
                .count();
        return Math.max(0.3, Math.min(1.0, (double) done / tasks.size()));
    }

    private double calculateScopeCreepRatio(Project project) {
        if (project.getStartDate() == null) return 0.0;
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());
        long initial = allTasks.stream()
                .filter(t -> !t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();
        if (initial == 0) return 0.0;
        long added = allTasks.stream()
                .filter(t -> t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();
        return Math.min(0.8, (double) added / initial);
    }

    private double calculateBudgetConsumptionRatio(Project project) {
        if (project.getBudget() == null || project.getBudget() <= 0) return 0.5;
        // Estimation basée sur le temps écoulé (Phase 1)
        // Phase 2 : récupérer le budget réellement consommé depuis Module 05
        double daysRatio = calculateDaysElapsedRatio(project);
        return Math.min(1.3, daysRatio * 1.1); // légère surestimation réaliste
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================

    private RiskSeverity determineSeverity(double probability) {
        if (probability >= THRESHOLD_CRITICAL) return RiskSeverity.CRITICAL;
        if (probability >= THRESHOLD_HIGH)     return RiskSeverity.HIGH;
        if (probability >= THRESHOLD_MEDIUM)   return RiskSeverity.MEDIUM;
        if (probability >= THRESHOLD_LOW)      return RiskSeverity.LOW;
        return null; // pas de risque
    }

    private String findCriticalFeature(
            double daysElapsedRatio, double tasksDoneRatio, int activeRisksCount,
            double bottleneckDaysAvg, double assigneePerfScore, double scopeCreepRatio) {

        // La feature la plus critique = celle qui contribue le plus négativement
        // Basé sur les feature importances du modèle entraîné
        double gap          = daysElapsedRatio - tasksDoneRatio; // gap temps/avancement
        double riskNorm     = activeRisksCount / 7.0;
        double bottleneckN  = bottleneckDaysAvg / 15.0;
        double perfRisk     = 1.0 - assigneePerfScore;
        double scopeN       = scopeCreepRatio;

        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("active_risks_count",  riskNorm     * 0.36); // poids RF
        scores.put("bottleneck_days_avg", bottleneckN  * 0.16);
        scores.put("tasks_done_ratio",    gap          * 0.12);
        scores.put("assignee_perf_score", perfRisk     * 0.08);
        scores.put("scope_creep_ratio",   scopeN       * 0.07);

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("active_risks_count");
    }

    private String buildMessage(Project project, double prob, RiskSeverity severity, String criticalFeature) {

        String featureLabel = switch (criticalFeature) {
            case "active_risks_count"  -> "un nombre élevé de signaux de risque actifs";
            case "bottleneck_days_avg" -> "des tâches bloquées trop longtemps sans mise à jour";
            case "tasks_done_ratio"    -> "un avancement insuffisant par rapport au temps écoulé";
            case "assignee_perf_score" -> "un historique de livraison insuffisant du freelancer assigné";
            case "scope_creep_ratio"   -> "un périmètre de projet qui a trop évolué après le démarrage";
            default                    -> criticalFeature;
        };

        int probPercent = (int) Math.round(prob * 100);

        // ✅ Projet sain — aucun risque
        if (severity == null) {
            return String.format(
                    "✅ Analyse IA — Projet \"%s\" : aucun risque de retard détecté. " +
                            "Le projet avance conformément aux prévisions (probabilité de retard : %d%%). " +
                            "Continuez sur cette lancée.",
                    project.getTitle(), probPercent
            );
        }

        // 🟡 Risque faible
        if (severity == RiskSeverity.LOW) {
            return String.format(
                    "🟡 Analyse IA — Projet \"%s\" : risque de retard faible détecté (%d%%). " +
                            "Facteur à surveiller : %s. " +
                            "Aucune action immédiate requise, mais une vigilance est recommandée.",
                    project.getTitle(), probPercent, featureLabel
            );
        }

        // 🟠 Risque moyen
        if (severity == RiskSeverity.MEDIUM) {
            return String.format(
                    "🟠 Analyse IA — Projet \"%s\" : risque de retard modéré détecté (%d%%). " +
                            "Facteur critique identifié par le modèle : %s. " +
                            "Une intervention corrective est recommandée pour maintenir les délais.",
                    project.getTitle(), probPercent, featureLabel
            );
        }

        // 🔴 Risque élevé
        if (severity == RiskSeverity.HIGH) {
            return String.format(
                    "🔴 Analyse IA — Projet \"%s\" : risque de retard élevé (%d%%). " +
                            "Le modèle Random Forest a identifié comme facteur critique : %s. " +
                            "Une action corrective immédiate est nécessaire pour éviter un dépassement des délais.",
                    project.getTitle(), probPercent, featureLabel
            );
        }

        // 🚨 Risque critique
        return String.format(
                "🚨 Analyse IA — Projet \"%s\" : risque de retard CRITIQUE (%d%%). " +
                        "Le modèle prédit avec une forte probabilité que ce projet ne sera pas livré dans les délais. " +
                        "Facteur déterminant : %s. " +
                        "Escalade immédiate requise auprès du client et du freelancer.",
                project.getTitle(), probPercent, featureLabel
        );
    }
}