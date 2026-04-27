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
                .build();
    }

    public TaskResponseDto toResponseDto(Task task) {
        if (task == null) {
            return null;
        }

        TaskResponseDto.TaskMemberDto assignedMemberDto = null;
        if (task.getAssignedMember() != null && task.getAssignedMember().getUser() != null) {
            assignedMemberDto = TaskResponseDto.TaskMemberDto.builder()
                    .memberId(task.getAssignedMember().getId())
                    .userId(task.getAssignedMember().getUser().getId())
                    .firstName(task.getAssignedMember().getUser().getFirstName())
                    .lastName(task.getAssignedMember().getUser().getLastName())
                    .photo(task.getAssignedMember().getUser().getPhoto())
                    .build();
        }

        return TaskResponseDto.builder()
                .id(task.getId())
                .agencyId(task.getAgency() != null ? task.getAgency().getId() : null)
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedMember(assignedMemberDto)
                .createdById(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
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
    }
}