package tn.esprit.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.post.PostStatus;
import tn.esprit.community.post.PostType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private PostType type;
    private String mediaUrl;
    private String fileUrl;
    private Long createdBy;
    private Long communityId;
    private PostStatus status;
    private boolean isAiGenerated;
    private boolean isValidated;
    private int reportCount;
}
