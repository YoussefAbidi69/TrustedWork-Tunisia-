package tn.esprit.userservice.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class SendMessageDTO {
    @NotBlank
    @Size(max = 2000)
    private String message;
    private Long replyToId;
    private Long taskRefId;
    private List<AttachmentRefDTO> attachments;
}
