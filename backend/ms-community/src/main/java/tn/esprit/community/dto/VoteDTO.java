package tn.esprit.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.Enum.VoteType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteDTO {
    private Long id;
    private Long postId;
    private Long userId;
    private VoteType type;
}
