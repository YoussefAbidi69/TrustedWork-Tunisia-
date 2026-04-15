package tn.esprit.community.dto.lms;

import lombok.Data;

@Data
public class ProgressWriteDTO {
    private Long userId;
    private Long lessonId;
    private boolean completed;
}
