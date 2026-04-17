package tn.esprit.msprojectservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.msprojectservice.dto.MLPredictionDTO;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.repositories.*;
import tn.esprit.msprojectservice.services.IMLPredictionService;
import tn.esprit.msprojectservice.services.INotificationService;
import tn.esprit.msprojectservice.services.IProgressReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor  // ✅ Fix 2 — remplace tous les @Autowired
public class DeliveryRiskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryRiskScheduler.class);

    private static final int BOTTLENECK_THRESHOLD_DAYS = 3;
    private static final int INACTIVITY_THRESHOLD_DAYS = 5;
    private static final double SCOPE_CREEP_THRESHOLD  = 0.30;

    // ✅ Fix 1 — tous les champs sont final, plus de @Autowired
    private final IProjectRepository     projectRepository;
    private final ITaskRepository        taskRepository;
    private final IRiskSignalRepository  riskSignalRepository;
    private final IDeliverableRepository deliverableRepository;
    private final IProgressReportService progressReportService;
    private final INotificationService   notificationService;
    private final IMLPredictionService   mlPredictionService;

    // ✅ Fix 1 — DELAY_RISK_THRESHOLD supprimé (champ privé jamais utilisé)
    // ✅ Fix 3,4 — calculateAssigneeScore() et determineSeverity() supprimées (méthodes privées jamais appelées)

    // ============================================================
    // ANALYSE QUOTIDIENNE
    // ============================================================
    @Scheduled(cron = "0 */15 * * * *")
    public void analyzeAllActiveProjects() {
        logger.info("========== DÉBUT ANALYSE IA QUOTIDIENNE ==========");

        List<Project> activeProjects = projectRepository.findAllActiveProjects();
        logger.info("Nombre de projets actifs à analyser : {}", activeProjects.size());

        for (Project project : activeProjects) {
            logger.info("--- Analyse du projet : {} (ID: {}) ---", project.getTitle(), project.getId());
            try {
                detectDelayRisk(project);
                detectBottleneck(project);
                detectInactivity(project);
                detectScopeCreep(project);
            } catch (Exception e) {
                logger.error("Erreur lors de l'analyse du projet {} : {}", project.getId(), e.getMessage());
            }
        }

        logger.info("========== FIN ANALYSE IA QUOTIDIENNE ==========");
    }

    // ============================================================
    // RAPPORT HEBDOMADAIRE
    // ============================================================
    @Scheduled(cron = "0 */15 * * * *")
    public void generateWeeklyReports() {
        logger.info("========== GÉNÉRATION RAPPORTS HEBDOMADAIRES ==========");

        for (Project project : projectRepository.findAllActiveProjects()) {
            try {
                ProgressReportDTO report = progressReportService.generateReport(project.getId());
                logger.info("Rapport généré pour le projet {} — Complétion : {}%",
                        project.getTitle(), report.getCompletionRate());
            } catch (Exception e) {
                logger.error("Erreur génération rapport pour le projet {} : {}", project.getId(), e.getMessage());
            }
        }

        logger.info("========== FIN GÉNÉRATION RAPPORTS ==========");
    }

    // ============================================================
    // 1. DELAY_RISK
    // ============================================================
    private void detectDelayRisk(Project project) {
        MLPredictionDTO prediction = mlPredictionService.predictDeliveryRisk(project.getId());

        if (prediction.getSeverity() == null) {
            logger.info("✅ Aucun DELAY_RISK — Projet: {} | P(retard): {}",
                    project.getTitle(), prediction.getProbabilityLate());
            return;
        }

        if (riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.DELAY_RISK)) {
            return;
        }

        createSignal(project, RiskType.DELAY_RISK, prediction.getSeverity(), prediction.getMessage(), null);

        logger.warn("🤖 DELAY_RISK ML — Projet: {} | P(retard): {}% | Sévérité: {}",
                project.getTitle(),
                Math.round(prediction.getProbabilityLate() * 100),
                prediction.getSeverity()
        );
    }

    // ============================================================
    // 2. BOTTLENECK
    // ============================================================
    private void detectBottleneck(Project project) {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByProjectId(project.getId());

        for (Task task : blockedTasks) {
            long daysSinceUpdate = ChronoUnit.DAYS.between(task.getUpdatedAt().toLocalDate(), LocalDate.now());

            // ✅ Fix 5 — if imbriqués fusionnés en une seule condition
            if (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS
                    && !riskSignalRepository.existsActiveSignal(project.getId(), RiskType.BOTTLENECK, task.getId())) {

                RiskSeverity severity = (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS * 2)
                        ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

                String statusLabel = (task.getStatus() == TaskStatus.IN_PROGRESS) ? "EN COURS" : "EN REVIEW";

                String message = String.format(
                        "Goulot d'étranglement détecté ! La tâche \"%s\" est bloquée en %s depuis %d jours sans mise à jour.",
                        task.getTitle(), statusLabel, daysSinceUpdate
                );

                createSignal(project, RiskType.BOTTLENECK, severity, message, task.getId());
                logger.warn("BOTTLENECK détecté — Tâche: {} — Bloquée depuis {} jours", task.getTitle(), daysSinceUpdate);
            }
        }
    }

    // ============================================================
    // 3. INACTIVITY
    // ============================================================
    private void detectInactivity(Project project) {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());

        if (allTasks.isEmpty()) {
            return;
        }

        LocalDateTime lastActivity = allTasks.stream()
                .map(Task::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(project.getCreatedAt());

        long daysSinceActivity = ChronoUnit.DAYS.between(lastActivity.toLocalDate(), LocalDate.now());

        // ✅ Fix 5 — if imbriqués fusionnés
        if (daysSinceActivity >= INACTIVITY_THRESHOLD_DAYS
                && !riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.INACTIVITY)) {

            RiskSeverity severity = (daysSinceActivity >= INACTIVITY_THRESHOLD_DAYS * 2)
                    ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

            String message = String.format(
                    "Inactivité détectée sur le projet \"%s\". Aucune tâche n'a été mise à jour depuis %d jours. Dernière activité : %s.",
                    project.getTitle(), daysSinceActivity, lastActivity.toLocalDate()
            );

            createSignal(project, RiskType.INACTIVITY, severity, message, null);
            logger.warn("INACTIVITY détecté — Projet: {} — Inactif depuis {} jours", project.getTitle(), daysSinceActivity);
        }
    }

    // ============================================================
    // 4. SCOPE_CREEP
    // ============================================================
    private void detectScopeCreep(Project project) {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());

        if (allTasks.isEmpty() || project.getStartDate() == null) {
            return;
        }

        long initialTasks = allTasks.stream()
                .filter(t -> !t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();

        if (initialTasks == 0) {
            return;
        }

        long addedTasks = allTasks.stream()
                .filter(t -> t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();

        double scopeRatio = (double) addedTasks / initialTasks;

        // ✅ Fix 5 — if imbriqués fusionnés
        if (scopeRatio > SCOPE_CREEP_THRESHOLD
                && !riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.SCOPE_CREEP)) {

            RiskSeverity severity = (scopeRatio > 0.60) ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

            String message = String.format(
                    "Scope Creep détecté sur le projet \"%s\" ! %d tâche(s) ajoutée(s) après le démarrage sur %d tâche(s) initiale(s) (ratio : %.0f%%). Seuil autorisé : 30%%.",
                    project.getTitle(), addedTasks, initialTasks, scopeRatio * 100
            );

            createSignal(project, RiskType.SCOPE_CREEP, severity, message, null);
            logger.warn("SCOPE_CREEP détecté — Projet: {} — Ratio: {}%", project.getTitle(), scopeRatio * 100);
        }
    }

    // ============================================================
    // NOTIFICATIONS QUOTIDIENNES
    // ============================================================
    @Scheduled(cron = "0 */15 * * * *")
    public void sendDailyNotifications() {
        logger.info("========== ENVOI NOTIFICATIONS QUOTIDIENNES ==========");

        for (Project project : projectRepository.findAllActiveProjects()) {
            try {
                checkDeadline24h(project);
                checkPendingDeliverables(project);
                checkBlockedTasks(project);
            } catch (Exception e) {
                logger.error("Erreur notifications pour le projet {} : {}", project.getId(), e.getMessage());
            }
        }

        logger.info("========== FIN NOTIFICATIONS ==========");
    }

    private void checkDeadline24h(Project project) {
        List<Task> tasks = taskRepository.findByProjectId(project.getId());

        for (Task task : tasks) {
            if (task.getDeadline() == null || task.getStatus() == TaskStatus.DONE) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());

            if (daysRemaining == 1 || daysRemaining == 0) {
                String title = "Deadline imminente !";
                String message = String.format(
                        "La tâche \"%s\" du projet \"%s\" arrive à échéance %s !",
                        task.getTitle(), project.getTitle(),
                        daysRemaining == 0 ? "aujourd'hui" : "demain"
                );

                if (task.getAssigneeId() != null) {
                    notificationService.createNotification(
                            task.getAssigneeId(), title, message,
                            NotificationType.DEADLINE_24H, project.getId(), task.getId()
                    );
                }

                notificationService.createNotification(
                        project.getClientId(), title, message,
                        NotificationType.DEADLINE_24H, project.getId(), task.getId()
                );

                logger.info("Notification DEADLINE_24H envoyée — Tâche: {}", task.getTitle());
            }
        }
    }

    private void checkPendingDeliverables(Project project) {
        int pendingCount = deliverableRepository.countOpenDeliverablesByProjectId(project.getId());

        if (pendingCount > 0) {
            String message = String.format(
                    "Vous avez %d livrable(s) en attente de validation sur le projet \"%s\".",
                    pendingCount, project.getTitle()
            );

            notificationService.createNotification(
                    project.getClientId(), "Livrable(s) en attente", message,
                    NotificationType.DELIVERABLE_PENDING, project.getId(), null
            );

            logger.info("Notification DELIVERABLE_PENDING envoyée — Projet: {} — {} en attente",
                    project.getTitle(), pendingCount);
        }
    }

    private void checkBlockedTasks(Project project) {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByProjectId(project.getId());

        for (Task task : blockedTasks) {
            long daysSinceUpdate = ChronoUnit.DAYS.between(task.getUpdatedAt().toLocalDate(), LocalDate.now());

            // ✅ Fix 5 — if imbriqués fusionnés
            if (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS && task.getAssigneeId() != null) {
                String message = String.format(
                        "La tâche \"%s\" est bloquée depuis %d jours sur le projet \"%s\".",
                        task.getTitle(), daysSinceUpdate, project.getTitle()
                );

                notificationService.createNotification(
                        task.getAssigneeId(), "Tâche bloquée", message,
                        NotificationType.TASK_BLOCKED, project.getId(), task.getId()
                );

                logger.info("Notification TASK_BLOCKED envoyée — Tâche: {}", task.getTitle());
            }
        }
    }

    private void createSignal(Project project, RiskType riskType, RiskSeverity severity,
                              String message, Long affectedTaskId) {
        DeliveryRiskSignal signal = DeliveryRiskSignal.builder()
                .project(project)
                .riskType(riskType)
                .severity(severity)
                .message(message)
                .affectedTaskId(affectedTaskId)
                .build();

        riskSignalRepository.save(signal);
        logger.info("Signal créé — Type: {} | Sévérité: {} | Projet: {}", riskType, severity, project.getTitle());
    }

    public void analyzeProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'id : " + projectId));

        logger.info("--- Analyse IA manuelle du projet : {} (ID: {}) ---", project.getTitle(), project.getId());

        detectDelayRisk(project);
        detectBottleneck(project);
        detectInactivity(project);
        detectScopeCreep(project);

        logger.info("--- Analyse IA terminée pour le projet : {} ---", project.getTitle());
    }
}