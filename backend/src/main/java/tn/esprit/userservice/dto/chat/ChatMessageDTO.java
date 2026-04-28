package tn.esprit.userservice.dto.chat;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChatMessageDTO {
    private Long id;
    private Long agencyId;
    private Long senderId;
    private String senderFirstName;
    private String senderLastName;
    private String senderPhoto;
    private String senderRole;
    private String message;
    private String sentAt;
    private boolean isDeleted;
    private boolean isPinned;
    
    private List<AttachmentDTO> attachments;
    private TaskCardDTO taskRef;
    private ReplyPreviewDTO replyTo;
    private List<ReactionSummaryDTO> reactions;
}
