package tn.esprit.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.Enum.BlockType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockRequest {
    private String title;
    private String content;
    private String fileUrl;
    private Integer orderIndex;
    private BlockType type;
}
