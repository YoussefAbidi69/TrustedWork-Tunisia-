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
    private String id;
    private String otherPartyId;
    private String otherPartyName;
    private String jobTitle;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private Long jobOfferId;
    private Long peerId;
    private String peerName;
}
