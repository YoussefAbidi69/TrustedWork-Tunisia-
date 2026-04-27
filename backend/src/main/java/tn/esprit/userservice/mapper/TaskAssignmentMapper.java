package tn.esprit.userservice.mapper;


import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.TaskAssignmentResponseDto;
import tn.esprit.userservice.entity.TaskAssignment;

@Component
public class TaskAssignmentMapper {

    public TaskAssignmentResponseDto toResponseDto(TaskAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        return TaskAssignmentResponseDto.builder()
                .id(assignment.getId())
                .taskId(assignment.getTask().getId())
                .memberId(assignment.getMember().getId())
                .completionScore(assignment.getCompletionScore())
                .assignedAt(assignment.getAssignedAt())
                .completedAt(assignment.getCompletedAt())
                .build();
    }
}