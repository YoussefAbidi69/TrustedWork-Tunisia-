package tn.esprit.msprojectservice.dto;

import lombok.*;
import tn.esprit.msprojectservice.entities.ProgressReport;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReportDTO {

    private Long id;
    private Long projectId;
    private int completedTasks;
    private int totalTasks;
    private int completionRate;
    private int openDeliverables;
    private int activeRisks;
    private String summary;
    private LocalDateTime generatedAt;

    // --- Mapping Entity -> DTO ---
    public static ProgressReportDTO fromEntity(ProgressReport report) {
        return ProgressReportDTO.builder()
                .id(report.getId())
                .projectId(report.getProject() != null ? report.getProject().getId() : null)
                .completedTasks(report.getCompletedTasks())
                .totalTasks(report.getTotalTasks())
                .completionRate(report.getCompletionRate())
                .openDeliverables(report.getOpenDeliverables())
                .activeRisks(report.getActiveRisks())
                .summary(report.getSummary())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
}