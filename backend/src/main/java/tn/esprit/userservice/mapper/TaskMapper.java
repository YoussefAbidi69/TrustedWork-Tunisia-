package tn.esprit.userservice.mapper;


import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.TaskRequestDto;
import tn.esprit.userservice.dto.TaskResponseDto;
import tn.esprit.userservice.dto.TaskUpdateDto;
import tn.esprit.userservice.entity.Task;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .requiredSkills(dto.getRequiredSkills())
                .dueDate(dto.getDueDate())
                .build();
    }

    public TaskResponseDto toResponseDto(Task task) {
        if (task == null) {
            return null;
        }

        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .requiredSkills(task.getRequiredSkills())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(TaskUpdateDto dto, Task task) {
        if (dto == null || task == null) {
            return;
        }

        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }

        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus());
        }

        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority());
        }

        if (dto.getRequiredSkills() != null) {
            task.setRequiredSkills(dto.getRequiredSkills());
        }

        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }
    }
}