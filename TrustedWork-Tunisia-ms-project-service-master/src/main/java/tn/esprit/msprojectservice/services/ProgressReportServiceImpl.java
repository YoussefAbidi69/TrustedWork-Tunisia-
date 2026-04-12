package tn.esprit.msprojectservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.entities.ProgressReport;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.repositories.IDeliverableRepository;
import tn.esprit.msprojectservice.repositories.IProgressReportRepository;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.repositories.IRiskSignalRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgressReportServiceImpl implements IProgressReportService {

    @Autowired
    private IProgressReportRepository progressReportRepository;

    @Autowired
    private IProjectRepository projectRepository;

    @Autowired
    private ITaskRepository taskRepository;

    @Autowired
    private IDeliverableRepository deliverableRepository;

    @Autowired
    private IRiskSignalRepository riskSignalRepository;

    @Override
    public ProgressReportDTO generateReport(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        // Calcul des métriques
        int totalTasks = taskRepository.countTotalTasksByProjectId(projectId);
        int completedTasks = taskRepository.countCompletedTasksByProjectId(projectId);
        int completionRate = (totalTasks > 0) ? (completedTasks * 100 / totalTasks) : 0;
        int openDeliverables = deliverableRepository.countOpenDeliverablesByProjectId(projectId);
        int activeRisks = riskSignalRepository.countActiveRisksByProjectId(projectId);

        // Génération du résumé automatique
        String summary = generateSummary(project.getTitle(), completedTasks, totalTasks, completionRate, openDeliverables, activeRisks);

        // Création du rapport
        ProgressReport report = ProgressReport.builder()
                .project(project)
                .completedTasks(completedTasks)
                .totalTasks(totalTasks)
                .completionRate(completionRate)
                .openDeliverables(openDeliverables)
                .activeRisks(activeRisks)
                .summary(summary)
                .build();

        ProgressReport saved = progressReportRepository.save(report);
        return ProgressReportDTO.fromEntity(saved);
    }

    @Override
    public ProgressReportDTO getLatestReport(Long projectId) {
        ProgressReport report = progressReportRepository.findLatestByProjectId(projectId)
                .orElseThrow(() -> new RuntimeException("Aucun rapport trouvé pour le projet : " + projectId));
        return ProgressReportDTO.fromEntity(report);
    }

    @Override
    public List<ProgressReportDTO> getReportHistory(Long projectId) {
        return progressReportRepository.findByProjectIdOrderByGeneratedAtDesc(projectId)
                .stream()
                .map(ProgressReportDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // --- Génération automatique du résumé textuel ---
    private String generateSummary(String projectTitle, int completedTasks, int totalTasks, int completionRate, int openDeliverables, int activeRisks) {
        StringBuilder sb = new StringBuilder();

        sb.append("Rapport de progression — ").append(projectTitle).append(". ");
        sb.append("Avancement global : ").append(completionRate).append("% ");
        sb.append("(").append(completedTasks).append("/").append(totalTasks).append(" tâches terminées). ");

        if (openDeliverables > 0) {
            sb.append(openDeliverables).append(" livrable(s) en attente de validation. ");
        } else {
            sb.append("Tous les livrables ont été traités. ");
        }

        if (activeRisks > 0) {
            sb.append("⚠ ").append(activeRisks).append(" signal(aux) de risque actif(s) détecté(s). ");
        } else {
            sb.append("Aucun risque actif détecté. ");
        }

        if (completionRate == 100) {
            sb.append("✅ Le projet est terminé avec succès !");
        } else if (completionRate >= 75) {
            sb.append("Le projet est en bonne voie de finalisation.");
        } else if (completionRate >= 50) {
            sb.append("Le projet progresse normalement.");
        } else if (completionRate >= 25) {
            sb.append("Le projet est encore en phase initiale.");
        } else {
            sb.append("Le projet vient de démarrer.");
        }

        return sb.toString();
    }
}