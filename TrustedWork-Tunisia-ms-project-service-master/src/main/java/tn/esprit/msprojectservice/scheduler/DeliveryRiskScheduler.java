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
import tn.esprit.msprojectservice.services.IMailService;
import tn.esprit.msprojectservice.services.IMLPredictionService;
import tn.esprit.msprojectservice.services.INotificationService;
import tn.esprit.msprojectservice.services.IProgressReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryRiskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryRiskScheduler.class);

    private static final int    BOTTLENECK_THRESHOLD_DAYS = 3;
    private static final int    INACTIVITY_THRESHOLD_DAYS = 5;
    private static final double SCOPE_CREEP_THRESHOLD     = 0.30;

    private final IProjectRepository     projectRepository;
    private final ITaskRepository        taskRepository;
    private final IRiskSignalRepository  riskSignalRepository;
    private final IDeliverableRepository deliverableRepository;
    private final IProgressReportService progressReportService;
    private final INotificationService   notificationService;
    private final IMLPredictionService   mlPredictionService;

    // MAILING -- Injection du service mail
    private final IMailService mailService;

    // ============================================================
    // ANALYSE QUOTIDIENNE
    // ============================================================
    @Scheduled(cron = "0 */15 * * * *")
    public void analyzeAllActiveProjects() {
        logger.info("========== DEBUT ANALYSE IA QUOTIDIENNE ==========");

        List<Project> activeProjects = projectRepository.findAllActiveProjects();
        logger.info("Nombre de projets actifs a analyser : {}", activeProjects.size());

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
    // RAPPORT HEBDOMADAIRE -- chaque lundi a 8h
    // ============================================================

    /**
     * Genere le rapport hebdomadaire pour chaque projet ACTIVE
     * et envoie un email recapitulatif au client ET au freelancer.
     *
     * Cron production : "0 0 8 * * MON"
     * Cron de test    : "0 * /15 * * * *" (toutes les 15 minutes)
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void generateWeeklyReports() {
        logger.info("========== GENERATION RAPPORTS HEBDOMADAIRES ==========");

        for (Project project : projectRepository.findAllActiveProjects()) {
            try {
                // 1. Generer le rapport en base
                ProgressReportDTO report = progressReportService.generateReport(project.getId());
                logger.info("Rapport genere pour le projet {} -- Completion : {}%",
                        project.getTitle(), report.getCompletionRate());

                // MAILING -- 2. Envoyer le rapport par email au client + freelancer
                sendWeeklyReportEmail(project, report);

            } catch (Exception e) {
                logger.error("Erreur generation rapport pour le projet {} : {}", project.getId(), e.getMessage());
            }
        }

        logger.info("========== FIN GENERATION RAPPORTS ==========");
    }

    private void sendWeeklyReportEmail(Project project, ProgressReportDTO report) {
        try {
            mailService.envoyerRapportHebdomadaire(project, report);
            logger.info("Email rapport hebdomadaire envoye -- Projet: {} | Client: {} | Freelancer: {}",
                    project.getTitle(), project.getClientId(), project.getFreelancerId());
        } catch (Exception mailException) {
            // L'erreur mail ne bloque pas le traitement des autres projets
            logger.error("Echec envoi email rapport pour le projet {} : {}",
                    project.getId(), mailException.getMessage());
        }
    }

    // ============================================================
    // 1. DELAY_RISK
    // ============================================================
    private void detectDelayRisk(Project project) {
        MLPredictionDTO prediction = mlPredictionService.predictDeliveryRisk(project.getId());

        if (prediction.getSeverity() == null) {
            logger.info("Aucun DELAY_RISK -- Projet: {} | P(retard): {}",
                    project.getTitle(), prediction.getProbabilityLate());
            return;
        }

        if (riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.DELAY_RISK)) {
            return;
        }

        createSignal(project, RiskType.DELAY_RISK, prediction.getSeverity(), prediction.getMessage(), null);

        logger.warn("DELAY_RISK ML -- Projet: {} | P(retard): {}% | Severite: {}",
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

            if (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS
                    && !riskSignalRepository.existsActiveSignal(project.getId(), RiskType.BOTTLENECK, task.getId())) {

                RiskSeverity severity = (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS * 2)
                        ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

                String statusLabel = (task.getStatus() == TaskStatus.IN_PROGRESS) ? "EN COURS" : "EN REVIEW";

                String message = String.format(
                        "Goulot d'etranglement detecte ! La tache \"%s\" est bloquee en %s depuis %d jours.",
                        task.getTitle(), statusLabel, daysSinceUpdate
                );

                createSignal(project, RiskType.BOTTLENECK, severity, message, task.getId());
                logger.warn("BOTTLENECK detecte -- Tache: {} -- Bloquee depuis {} jours",
                        task.getTitle(), daysSinceUpdate);
            }
        }
    }

    // ============================================================
    // 3. INACTIVITY
    // ============================================================
    private void detectInactivity(Project project) {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());

        if (allTasks.isEmpty()) return;

        LocalDateTime lastActivity = allTasks.stream()
                .map(Task::getUpdatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(project.getCreatedAt());

        long daysSinceActivity = ChronoUnit.DAYS.between(lastActivity.toLocalDate(), LocalDate.now());

        if (daysSinceActivity >= INACTIVITY_THRESHOLD_DAYS
                && !riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.INACTIVITY)) {

            RiskSeverity severity = (daysSinceActivity >= INACTIVITY_THRESHOLD_DAYS * 2)
                    ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

            String message = String.format(
                    "Inactivite detectee sur le projet \"%s\". Aucune tache n'a ete mise a jour depuis %d jours.",
                    project.getTitle(), daysSinceActivity
            );

            createSignal(project, RiskType.INACTIVITY, severity, message, null);
            logger.warn("INACTIVITY detecte -- Projet: {} -- Inactif depuis {} jours",
                    project.getTitle(), daysSinceActivity);
        }
    }

    // ============================================================
    // 4. SCOPE_CREEP
    // ============================================================
    private void detectScopeCreep(Project project) {
        List<Task> allTasks = taskRepository.findByProjectId(project.getId());

        if (allTasks.isEmpty() || project.getStartDate() == null) return;

        long initialTasks = allTasks.stream()
                .filter(t -> !t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();

        if (initialTasks == 0) return;

        long addedTasks = allTasks.stream()
                .filter(t -> t.getCreatedAt().toLocalDate().isAfter(project.getStartDate()))
                .count();

        double scopeRatio = (double) addedTasks / initialTasks;

        if (scopeRatio > SCOPE_CREEP_THRESHOLD
                && !riskSignalRepository.existsActiveProjectSignal(project.getId(), RiskType.SCOPE_CREEP)) {

            RiskSeverity severity = (scopeRatio > 0.60) ? RiskSeverity.HIGH : RiskSeverity.MEDIUM;

            String message = String.format(
                    "Scope Creep detecte sur le projet \"%s\" ! %d tache(s) ajoutee(s) apres le demarrage (ratio : %.0f%%).",
                    project.getTitle(), addedTasks, scopeRatio * 100
            );

            createSignal(project, RiskType.SCOPE_CREEP, severity, message, null);
            logger.warn("SCOPE_CREEP detecte -- Projet: {} -- Ratio: {}%",
                    project.getTitle(), scopeRatio * 100);
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
            if (task.getDeadline() == null || task.getStatus() == TaskStatus.DONE) continue;

            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());

            if (daysRemaining == 1 || daysRemaining == 0) {
                String title = "Deadline imminente !";
                String message = String.format(
                        "La tache \"%s\" du projet \"%s\" arrive a echeance %s !",
                        task.getTitle(), project.getTitle(),
                        daysRemaining == 0 ? "aujourd'hui" : "demain"
                );

                if (task.getAssigneeId() != null) {
                    notificationService.createNotification(
                            task.getAssigneeId(), title, message,
                            NotificationType.DEADLINE_24H, project.getId(), task.getId());
                }
                notificationService.createNotification(
                        project.getClientId(), title, message,
                        NotificationType.DEADLINE_24H, project.getId(), task.getId());

                logger.info("Notification DEADLINE_24H envoyee -- Tache: {}", task.getTitle());
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
                    NotificationType.DELIVERABLE_PENDING, project.getId(), null);

            logger.info("Notification DELIVERABLE_PENDING envoyee -- Projet: {}", project.getTitle());
        }
    }

    private void checkBlockedTasks(Project project) {
        List<Task> blockedTasks = taskRepository.findBlockedTasksByProjectId(project.getId());

        for (Task task : blockedTasks) {
            long daysSinceUpdate = ChronoUnit.DAYS.between(task.getUpdatedAt().toLocalDate(), LocalDate.now());

            if (daysSinceUpdate >= BOTTLENECK_THRESHOLD_DAYS && task.getAssigneeId() != null) {
                String message = String.format(
                        "La tache \"%s\" est bloquee depuis %d jours sur le projet \"%s\".",
                        task.getTitle(), daysSinceUpdate, project.getTitle()
                );
                notificationService.createNotification(
                        task.getAssigneeId(), "Tache bloquee", message,
                        NotificationType.TASK_BLOCKED, project.getId(), task.getId());

                logger.info("Notification TASK_BLOCKED envoyee -- Tache: {}", task.getTitle());
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
        logger.info("Signal cree -- Type: {} | Severite: {} | Projet: {}",
                riskType, severity, project.getTitle());
    }

    public void analyzeProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Projet non trouve avec l'id : " + projectId));

        logger.info("--- Analyse IA manuelle du projet : {} (ID: {}) ---",
                project.getTitle(), project.getId());

        detectDelayRisk(project);
        detectBottleneck(project);
        detectInactivity(project);
        detectScopeCreep(project);

        logger.info("--- Analyse IA terminee pour le projet : {} ---", project.getTitle());
    }
}