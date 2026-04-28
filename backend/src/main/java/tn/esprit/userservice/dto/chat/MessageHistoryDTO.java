package tn.esprit.userservice.dto.chat;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MessageHistoryDTO {
    private Long id;
    private Long senderId;
    private String senderFirstName;
    private String senderLastName;
    private String senderPhoto;
    private String senderRole;
    private String message;
    private String attachmentUrl;
    private String sentAt;
    private boolean deleted;
    private boolean isPinned;
    private List<AttachmentDTO> attachments;
    private TaskCardDTO taskRef;
    private ReplyPreviewDTO replyTo;
    private List<ReactionSummaryDTO> reactions;
}
