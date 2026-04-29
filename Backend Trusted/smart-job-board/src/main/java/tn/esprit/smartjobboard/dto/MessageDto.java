package tn.esprit.smartjobboard.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private String senderId;
    private String content;
    private String type;
    private String fileUrl;
    private String meetUrl;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private boolean read;
}
