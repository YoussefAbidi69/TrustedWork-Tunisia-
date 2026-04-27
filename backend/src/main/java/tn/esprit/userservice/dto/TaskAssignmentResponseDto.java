package tn.esprit.userservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentResponseDto {

    private Long id;
    private Long taskId;
    private Long memberId;
    private Float completionScore;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
}