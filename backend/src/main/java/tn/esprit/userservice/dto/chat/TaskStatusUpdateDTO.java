package tn.esprit.userservice.dto.chat;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateDTO {
    private Long taskId;
    private String newStatus;
    private LocalDateTime updatedAt;
}
