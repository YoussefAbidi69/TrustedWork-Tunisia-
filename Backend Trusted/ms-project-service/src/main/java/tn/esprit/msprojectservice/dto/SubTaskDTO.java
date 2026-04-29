package tn.esprit.msprojectservice.dto;

import lombok.*;
import tn.esprit.msprojectservice.entities.SubTask;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskDTO {

    private Long id;
    private String title;
    private boolean done;
    private Long taskId;
    private LocalDateTime createdAt;

    // --- Mapping Entity -> DTO ---
    public static SubTaskDTO fromEntity(SubTask subTask) {
        return SubTaskDTO.builder()
                .id(subTask.getId())
                .title(subTask.getTitle())
                .done(subTask.isDone())
                .taskId(subTask.getTask() != null ? subTask.getTask().getId() : null)
                .createdAt(subTask.getCreatedAt())
                .build();
    }

    // --- Mapping DTO -> Entity (sans le Task, setté dans le service) ---
    public static SubTask toEntity(SubTaskDTO dto) {
        return SubTask.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .done(dto.isDone())
                .build();
    }
}