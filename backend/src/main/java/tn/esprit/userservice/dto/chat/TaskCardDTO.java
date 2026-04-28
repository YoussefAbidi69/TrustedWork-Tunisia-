package tn.esprit.userservice.dto.chat;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCardDTO {
    private Long id;
    private String title;
    private String status;
    private String priority;
    private String assigneeName;
    private String assigneePhoto;
    private String dueDate;
    private String projectTitle;
    private int progressPercent;
}
