package tn.esprit.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.enums.PostStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private String title;
    private String content;
    private Long createdBy;
    private Long communityId;
    private PostStatus status;
}
