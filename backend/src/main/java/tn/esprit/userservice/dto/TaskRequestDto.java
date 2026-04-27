package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.TaskPriority;
import tn.esprit.userservice.entity.TaskStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDto {

    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assignedMemberId;
    private Long requesterId; // to know who is creating the task
}