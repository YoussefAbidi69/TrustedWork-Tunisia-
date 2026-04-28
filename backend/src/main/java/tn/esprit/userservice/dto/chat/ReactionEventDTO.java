package tn.esprit.userservice.dto.chat;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionEventDTO {
    private Long messageId;
    private String emoji;
    private Long userId;
    private boolean removed;
    private List<ReactionSummaryDTO> updatedReactions;
}
