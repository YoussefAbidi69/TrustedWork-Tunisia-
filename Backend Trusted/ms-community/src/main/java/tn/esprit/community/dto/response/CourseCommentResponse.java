package tn.esprit.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCommentResponse {
    private Long id;
    private Long courseId;
    private Long userId;
    private String content;
}
