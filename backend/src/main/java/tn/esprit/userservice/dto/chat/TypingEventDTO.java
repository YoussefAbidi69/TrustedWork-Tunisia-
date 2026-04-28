package tn.esprit.userservice.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TypingEventDTO {
    private Long agencyId;
    private Long userId;
    private String firstName;
    private boolean isTyping;
}
