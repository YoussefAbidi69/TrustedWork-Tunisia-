package tn.esprit.userservice.dto;

import lombok.*;
import tn.esprit.userservice.entity.MemberRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyInvitationRequestDto {
    private Long agencyId;
    private Long receiverId; // Link to the user being invited
    private Long senderId;   // Link to the Lead sending the invite
    private MemberRole proposedRole;
    private String message;
}