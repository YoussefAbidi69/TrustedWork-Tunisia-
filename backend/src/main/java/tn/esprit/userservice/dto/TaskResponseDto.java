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
public class TaskResponseDto {

    private Long id;
    private Long agencyId;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskMemberDto {
        private Long memberId;
        private Long userId;
        private String firstName;
        private String lastName;
        private String photo;
    }

    private TaskMemberDto assignedMember;
    private Long createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}