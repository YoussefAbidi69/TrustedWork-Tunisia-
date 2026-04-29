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
public class SectionResponse {
    private Long id;
    private Long courseId;
    private String title;
    private int orderIndex;
    private List<BlockResponse> blocks;
}
