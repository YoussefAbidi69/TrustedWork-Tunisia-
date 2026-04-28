package tn.esprit.community.dto.lms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.enums.BlockType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockDTO {
    private Long id;
    private Long sectionId;
    private String title;
    private String content;
    private String fileUrl;
    private Integer orderIndex;
    private BlockType type;
}

