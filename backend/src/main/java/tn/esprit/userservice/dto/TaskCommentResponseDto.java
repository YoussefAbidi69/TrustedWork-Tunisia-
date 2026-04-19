package tn.esprit.userservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCommentResponseDto {

    private Long id;
    private Long taskId;
    private Long userId;
    private String content;
    private LocalDateTime commentedAt;
}