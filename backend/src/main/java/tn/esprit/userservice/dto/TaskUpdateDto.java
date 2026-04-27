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
public class TaskUpdateDto {

    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assignedMemberId;
    private Long projectId; // could change project
    private Long requesterId; // to know who is creating the task
}