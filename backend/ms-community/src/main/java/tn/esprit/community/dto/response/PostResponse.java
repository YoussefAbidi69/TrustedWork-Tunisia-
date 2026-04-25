package tn.esprit.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.VoteType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private Long createdBy;
    private Long communityId;
    private PostStatus status;
    private int reportCount;
    private long upvoteCount;
    private long downvoteCount;
    private VoteType myVote;
}
