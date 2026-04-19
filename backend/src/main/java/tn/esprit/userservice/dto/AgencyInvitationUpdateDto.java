package tn.esprit.userservice.dto;


import lombok.*;
import tn.esprit.userservice.entity.InvitationStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyInvitationUpdateDto {

    private InvitationStatus status;
}
