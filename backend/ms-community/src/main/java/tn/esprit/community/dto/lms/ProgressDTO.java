package tn.esprit.community.dto.lms;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private Long id;
    private Long userId;
    private Long lessonId;
    private boolean completed;
    private Instant completedAt;
}
