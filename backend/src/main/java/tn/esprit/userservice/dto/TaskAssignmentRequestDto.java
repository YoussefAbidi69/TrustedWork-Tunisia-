package tn.esprit.userservice.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentRequestDto {

    private Long taskId;
    private Long memberId;
}