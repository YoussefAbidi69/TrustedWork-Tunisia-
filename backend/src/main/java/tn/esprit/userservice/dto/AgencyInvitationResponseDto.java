package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.InvitationStatus;
import tn.esprit.userservice.entity.MemberRole;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyInvitationResponseDto {
    private Long id;
    private Long agencyId;
    private Long receiverId;
    private Long senderId;
    private MemberRole proposedRole;
    private InvitationStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime respondedAt;
    private String message;
    private String agencyName;
    private String senderName;
}