package tn.esprit.smartjobboard.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private Long jobOfferId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private boolean read;
    private LocalDateTime sentAt;
}
