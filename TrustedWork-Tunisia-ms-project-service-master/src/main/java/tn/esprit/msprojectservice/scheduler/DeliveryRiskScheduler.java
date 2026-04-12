package tn.esprit.msprojectservice.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.*;
import tn.esprit.msprojectservice.services.IProgressReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import tn.esprit.msprojectservice.services.INotificationService;
import tn.esprit.msprojectservice.entities.NotificationType;
@Component
public class DeliveryRiskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryRiskScheduler.class);

    // --- Seuils configurables ---
    private static final int BOTTLENECK_THRESHOLD_DAYS = 3;
    private static final int INACTIVITY_THRESHOLD_DAYS = 5;
    private static final double SCOPE_CREEP_THRESHOLD = 0.30;
    private static final double DELAY_RISK_THRESHOLD = 0.6;

    @Autowired
    private IProjectRepository projectRepository;

    @Autowired
    private ITaskRepository taskRepository;

    @Autowired
    private IRiskSignalRepository riskSignalRepository;

    @Autowired
    private IProgressReportService progressReportService;


    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IDeliverableRepository deliverableRepository;

    // ============================================================
    // ANALYSE QUOTIDIENNE — Tous les jours à 8h00
    // ============================================================
    @Scheduled(cron = "0 0 8 * * *")
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
    // RAPPORT HEBDOMADAIRE — Chaque lundi à 8h00
    // ============================================================
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklyReports() {
        logger.info("========== GÉNÉRATION RAPPORTS HEBDOMADAIRES ==========");

        List<Project> activeProjects = projectRepository.findAllActiveProjects();

        for (Project project : activeProjects) {
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
    // 1. DELAY_RISK — Détection de retard par tâche
    // ============================================================
    private void detectDelayRisk(Project project) {
        List<Task> tasks = taskRepository.findByProjectId(project.getId());

        for (Task task : tasks) {
            // Ignorer les tâches terminées ou sans deadline
            if (task.getStatus() == TaskStatus.DONE || task.getDeadline() == null) {
                continue;
            }

            // Calcul du score de risque de retard
            double riskScore = calculateDelayRiskScore(task, project);

            if (riskScore >= DELAY_RISK_THRESHOLD) {
                // Vérifier qu'un signal n'existe pas déjà pour cette tâche
                if (!riskSignalRepository.existsActiveSignal(project.getId(), RiskType.DELAY_RISK, task.getId())) {

                    RiskSeverity severity = determineSeverity(riskScore);

                    long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());

                    String message = String.format(
                            "⚠ Risque de retard détecté sur la tâche \"%s\". Score de risque : %.0f%%. " +
                                    "Jours restants : %d. Priorité : %s.",
                            task.getTitle(), riskScore * 100, daysRemaining, task.getPriority()
                    );

                    createSignal(project, RiskType.DELAY_RISK, severity, message, task.getId());
                    logger.warn("DELAY_RISK détecté — Tâche: {} — Score: {}", task.getTitle(), riskScore);
                }
            }
        }
    }

    // ============================================================
    // 2. BOTTLENECK — Tâche bloquée trop longtemps
    // ============================================================
    private void detectBottleneck(Project project) {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByProjectId(project.getId());

        for (Task task : blockedTasks) {
            long daysSinceUpdate = ChronoUnit.DAYS.between(task.getUpdatedAt().toLocalDate(), LocalDate.now());

            if (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS) {
                if (!riskSignalRepository.existsActiveSignal(project.getId(), RiskType.BOTTLENECK, task.getId())) {

                    RiskSeverity severity = (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS * 2)
                            ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

                    String statusLabel = (task.getStatus() == TaskStatus.IN_PROGRESS) ? "EN COURS" : "EN REVIEW";

                    String message = String.format(
                            "🔴 Goulot d'étranglement détecté ! La tâche \"%s\" est bloquée en %s " +
                                    "depuis %d jours sans mise à jour.",
                            task.getTitle(), statusLabel, daysSinceUpdate
                    );

                    createSignal(project, RiskType.BOTTLENECK, severity, message, task.getId());
                    logger.warn("BOTTLENECK détecté — Tâche: {} — Bloquée depuis {} jours", task.getTitle(), daysSinceUpdate);
                }
            }
        }
    }

    // ============================================================
    // 3. INACTIVITY — Aucune activité sur le projet
    // ============================================================
    private void detectInactivity(Project project) {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());

        if (allTasks.isEmpty()) {
            return;
        }

        // Trouver la date de dernière mise à jour parmi toutes les tâches
        LocalDateTime lastActivity = allTasks.stream()
                .map(Task::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(project.getCreatedAt());

        long daysSinceActivity = ChronoUnit.DAYS.between(lastActivity.toLocalDate(), LocalDate.now());

        if (daysSinceActivity >= INACTIVITY_THRESHOLD_DAYS) {
            if (!riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.INACTIVITY)) {

                RiskSeverity severity = (daysSinceActivity >= INACTIVITY_THRESHOLD_DAYS * 2)
                        ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

                String message = String.format(
                        "💤 Inactivité détectée sur le projet \"%s\". Aucune tâche n'a été mise à jour " +
                                "depuis %d jours. Dernière activité : %s.",
                        project.getTitle(), daysSinceActivity, lastActivity.toLocalDate()
                );

                createSignal(project, RiskType.INACTIVITY, severity, message, null);
                logger.warn("INACTIVITY détecté — Projet: {} — Inactif depuis {} jours", project.getTitle(), daysSinceActivity);
            }
        }
    }

    // ============================================================
    // 4. SCOPE_CREEP — Trop de tâches ajoutées après démarrage
    // ============================================================
    private void detectScopeCreep(Project project) {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());

        if (allTasks.isEmpty() || project.getStartDate() == null) {
            return;
        }

        // Tâches présentes au démarrage (créées le jour du startDate ou avant)
        long initialTasks = allTasks.stream()
                .filter(t -> !t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();

        // Tâches ajoutées après le démarrage
        long addedTasks = allTasks.stream()
                .filter(t -> t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();

        // Éviter la division par zéro
        if (initialTasks == 0) {
            return;
        }

        double scopeRatio = (double) addedTasks / initialTasks;

        if (scopeRatio > SCOPE_CREEP_THRESHOLD) {
            if (!riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.SCOPE_CREEP)) {

                RiskSeverity severity = (scopeRatio > 0.60) ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

                String message = String.format(
                        "📈 Scope Creep détecté sur le projet \"%s\" ! %d tâche(s) ajoutée(s) après le démarrage " +
                                "sur %d tâche(s) initiale(s) (ratio : %.0f%%). Seuil autorisé : 30%%.",
                        project.getTitle(), addedTasks, initialTasks, scopeRatio * 100
                );

                createSignal(project, RiskType.SCOPE_CREEP, severity, message, null);
                logger.warn("SCOPE_CREEP détecté — Projet: {} — Ratio: {}%", project.getTitle(), scopeRatio * 100);
            }
        }
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================

    /**
     * Calcul du score de risque de retard
     * DeliveryRisk = (daysRemaining / estimatedDuration × 0.35)
     *              + (taskCompletionRate × 0.35)
     *              + (assigneeHistoryScore × 0.30)
     */
    private double calculateDelayRiskScore(Task task, Project project) {
        // --- Facteur 1 : Ratio temps restant ---
        long totalDuration;
        if (task.getEstimatedHours() != null && task.getEstimatedHours() > 0) {
            totalDuration = task.getEstimatedHours() / 8; // Convertir heures en jours
            if (totalDuration == 0) totalDuration = 1;
        } else if (project.getStartDate() != null && task.getDeadline() != null) {
            totalDuration = ChronoUnit.DAYS.between(project.getStartDate(), task.getDeadline());
            if (totalDuration == 0) totalDuration = 1;
        } else {
            totalDuration = 30; // Valeur par défaut
        }

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());
        // Si deadline dépassée, le ratio est maximal (risque = 1.0)
        double timeRatio = (daysRemaining <= 0) ? 1.0 : 1.0 - ((double) daysRemaining / totalDuration);
        timeRatio = Math.max(0, Math.min(1, timeRatio)); // Borner entre 0 et 1

        // --- Facteur 2 : Taux de complétion des tâches du projet ---
        int totalTasks = taskRepository.countTotalTasksByProjectId(project.getId());
        int completedTasks = taskRepository.countCompletedTasksByProjectId(project.getId());
        double taskCompletionRate = (totalTasks > 0) ? 1.0 - ((double) completedTasks / totalTasks) : 0.5;

        // --- Facteur 3 : Score historique de l'assignee (simplifié en Phase 1) ---
        double assigneeHistoryScore = calculateAssigneeScore(task);

        // --- Score final ---
        return (timeRatio * 0.35) + (taskCompletionRate * 0.35) + (assigneeHistoryScore * 0.30);
    }

    /**
     * Score de fiabilité de l'assignee basé sur son historique
     * Phase 1 : basé sur les tâches complétées dans le même projet
     * Phase 2 : Feign Client vers Module 02 pour l'historique global
     */
    private double calculateAssigneeScore(Task task) {
        if (task.getAssigneeId() == null) {
            return 0.5; // Pas d'assignee = risque neutre
        }

        List<Task> assigneeTasks = taskRepository.findByAssigneeId(task.getAssigneeId());

        if (assigneeTasks.isEmpty()) {
            return 0.5; // Pas d'historique = risque neutre
        }

        long totalAssigneeTasks = assigneeTasks.size();
        long completedOnTime = assigneeTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .filter(t -> t.getDeadline() == null || !t.getUpdatedAt().toLocalDate().isAfter(t.getDeadline()))
                .count();

        // Plus le ratio est bas, plus le risque est élevé
        double reliabilityRate = (double) completedOnTime / totalAssigneeTasks;
        return 1.0 - reliabilityRate; // Inverser : fiabilité basse = risque haut
    }

    /**
     * Déterminer la sévérité en fonction du score
     */
    private RiskSeverity determineSeverity(double score) {
        if (score >= 0.85) return RiskSeverity.CRITICAL;
        if (score >= 0.75) return RiskSeverity.HIGH;
        if (score >= 0.60) return RiskSeverity.MEDIUM;
        return RiskSeverity.LOW;
    }

    /**
     * Créer et sauvegarder un signal de risque
     */
    private void createSignal(Project project, RiskType riskType, RiskSeverity severity, String message, Long affectedTaskId) {
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


    /**
     * Analyse manuelle d'un projet spécifique (appelé depuis le controller)
     */
    public void analyzeProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        logger.info("--- Analyse IA manuelle du projet : {} (ID: {}) ---", project.getTitle(), project.getId());

        detectDelayRisk(project);
        detectBottleneck(project);
        detectInactivity(project);
        detectScopeCreep(project);

        logger.info("--- Analyse IA terminée pour le projet : {} ---", project.getTitle());
    }


    // ============================================================
// NOTIFICATIONS QUOTIDIENNES — Tous les jours à 7h00
// ============================================================
    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyNotifications() {
        logger.info("========== ENVOI NOTIFICATIONS QUOTIDIENNES ==========");

        List<Project> activeProjects = projectRepository.findAllActiveProjects();

        for (Project project : activeProjects) {
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

    /**
     * Notifier les tâches dont la deadline est dans 24h
     */
    private void checkDeadline24h(Project project) {
        List<Task> tasks = taskRepository.findByProjectId(project.getId());

        for (Task task : tasks) {
            if (task.getDeadline() == null || task.getStatus() == TaskStatus.DONE) {
                continue;
            }

            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());

            if (daysRemaining == 1 || daysRemaining == 0) {
                String title = "⏰ Deadline imminente !";
                String message = String.format(
                        "La tâche \"%s\" du projet \"%s\" arrive à échéance %s !",
                        task.getTitle(), project.getTitle(),
                        daysRemaining == 0 ? "aujourd'hui" : "demain"
                );

                // Notifier le freelancer assigné
                if (task.getAssigneeId() != null) {
                    notificationService.createNotification(
                            task.getAssigneeId(), title, message,
                            NotificationType.DEADLINE_24H, project.getId(), task.getId()
                    );
                }

                // Notifier le client aussi
                notificationService.createNotification(
                        project.getClientId(), title, message,
                        NotificationType.DEADLINE_24H, project.getId(), task.getId()
                );

                logger.info("Notification DEADLINE_24H envoyée — Tâche: {}", task.getTitle());
            }
        }
    }

    /**
     * Notifier le client des livrables en attente de validation
     */
    private void checkPendingDeliverables(Project project) {
        int pendingCount = deliverableRepository.countOpenDeliverablesByProjectId(project.getId());

        if (pendingCount > 0) {
            String title = "📦 Livrable(s) en attente";
            String message = String.format(
                    "Vous avez %d livrable(s) en attente de validation sur le projet \"%s\".",
                    pendingCount, project.getTitle()
            );

            notificationService.createNotification(
                    project.getClientId(), title, message,
                    NotificationType.DELIVERABLE_PENDING, project.getId(), null
            );

            logger.info("Notification DELIVERABLE_PENDING envoyée — Projet: {} — {} en attente", project.getTitle(), pendingCount);
        }
    }

    /**
     * Notifier les tâches bloquées depuis trop longtemps
     */
    private void checkBlockedTasks(Project project) {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByProjectId(project.getId());

        for (Task task : blockedTasks) {
            long daysSinceUpdate = ChronoUnit.DAYS.between(task.getUpdatedAt().toLocalDate(), LocalDate.now());

            if (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS) {
                String title = "🔴 Tâche bloquée";
                String message = String.format(
                        "La tâche \"%s\" est bloquée depuis %d jours sur le projet \"%s\".",
                        task.getTitle(), daysSinceUpdate, project.getTitle()
                );

                // Notifier le freelancer
                if (task.getAssigneeId() != null) {
                    notificationService.createNotification(
                            task.getAssigneeId(), title, message,
                            NotificationType.TASK_BLOCKED, project.getId(), task.getId()
                    );
                }

                logger.info("Notification TASK_BLOCKED envoyée — Tâche: {}", task.getTitle());
            }
        }
    }
}