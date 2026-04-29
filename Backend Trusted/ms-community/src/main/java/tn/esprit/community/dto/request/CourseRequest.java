package tn.esprit.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {
    private String title;
    private String description;
    private Long authorId;
    private Long communityId;
    private Boolean published;
}
