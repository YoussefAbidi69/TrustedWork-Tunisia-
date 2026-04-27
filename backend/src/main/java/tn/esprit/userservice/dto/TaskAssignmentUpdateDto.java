package tn.esprit.userservice.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentUpdateDto {

    private Float completionScore;
}