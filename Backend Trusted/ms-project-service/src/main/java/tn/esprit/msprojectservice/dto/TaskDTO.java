package tn.esprit.msprojectservice.dto;

import lombok.*;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.entities.TaskStatus;
import tn.esprit.msprojectservice.entities.TaskPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long projectId;
    private Long assigneeId;
    private LocalDate deadline;
    private Integer estimatedHours;
    private Integer actualHours;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Mapping Entity -> DTO ---
    public static TaskDTO fromEntity(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .assigneeId(task.getAssigneeId())
                .deadline(task.getDeadline())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    // --- Mapping DTO -> Entity (sans le Project, il sera setté dans le service) ---
    public static Task toEntity(TaskDTO dto) {
        return Task.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .assigneeId(dto.getAssigneeId())
                .deadline(dto.getDeadline())
                .estimatedHours(dto.getEstimatedHours())
                .actualHours(dto.getActualHours())
                .build();
    }
}