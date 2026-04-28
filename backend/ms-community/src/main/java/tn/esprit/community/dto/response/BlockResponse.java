package tn.esprit.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.enums.BlockType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockResponse {
    private Long id;
    private Long sectionId;
    private String title;
    private String content;
    private String fileUrl;
    private int orderIndex;
    private BlockType type;
}
