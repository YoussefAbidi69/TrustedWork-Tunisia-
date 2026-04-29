package tn.esprit.community.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDownloadResponse {
    private Long id;
    private String title;
    private String description;
    private Long authorId;
    private Long communityId;
    private boolean published;
    private List<SectionResponse> sections;
}
