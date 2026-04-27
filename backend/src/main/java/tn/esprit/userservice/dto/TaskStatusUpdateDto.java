package tn.esprit.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.userservice.entity.TaskStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateDto {
    private TaskStatus status;
}
