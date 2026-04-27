package tn.esprit.smartjobboard.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Summary of a conversation thread for the conversation list view.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryDto {
    private Long jobOfferId;
    private String jobTitle;
    private Long peerId;
    private String peerName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
