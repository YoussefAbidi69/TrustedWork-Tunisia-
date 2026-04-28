package tn.esprit.userservice.dto.chat;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PresenceEventDTO {
    private Long agencyId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String photo;
    private String event; // "JOIN" | "LEAVE"
    private String timestamp;
    private List<OnlineMemberDTO> currentOnlineMembers;
}
