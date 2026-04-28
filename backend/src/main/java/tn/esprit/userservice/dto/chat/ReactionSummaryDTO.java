package tn.esprit.userservice.dto.chat;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryDTO {
    private String emoji;
    private int count;
    private List<Long> userIds;
    private boolean reactedByMe;
}
