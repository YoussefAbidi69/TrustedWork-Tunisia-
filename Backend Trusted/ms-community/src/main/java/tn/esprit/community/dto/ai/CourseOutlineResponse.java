package tn.esprit.community.dto.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOutlineResponse {
    private String topic;
    private String level;
    private List<SectionOutline> sections;
}
