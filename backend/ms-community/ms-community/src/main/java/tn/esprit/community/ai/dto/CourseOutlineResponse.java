package tn.esprit.community.ai.dto;

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
