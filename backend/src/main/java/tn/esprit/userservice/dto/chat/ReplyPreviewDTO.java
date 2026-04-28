package tn.esprit.userservice.dto.chat;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyPreviewDTO {
    private Long id;
    private String senderFirstName;
    private String senderLastName;
    private String messagePreview;
    private String sentAt;
    private boolean hasAttachments;
}
