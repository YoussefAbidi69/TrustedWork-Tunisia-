package tn.esprit.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.PostType;
import tn.esprit.community.entity.Enum.VoteType;

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
    /** Aggregated from votes table (not persisted on Post entity). */
    private int upvoteCount;
    /** Aggregated from votes table (not persisted on Post entity). */
    private int downvoteCount;

    /** Current user's vote on this post; set when {@code voterId} is passed to list/get. */
    private VoteType myVote;
}
