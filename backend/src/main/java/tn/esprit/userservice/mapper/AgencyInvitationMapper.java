package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.AgencyInvitationRequestDto;
import tn.esprit.userservice.dto.AgencyInvitationResponseDto;
import tn.esprit.userservice.entity.AgencyInvitation;

@Component
public class AgencyInvitationMapper {

    public AgencyInvitation toEntity(AgencyInvitationRequestDto dto) {
        if (dto == null) {
            return null;
        }

        // Note: sender, receiver, and agency relationships should be mapped 
        // in the Service layer where repositories are available.
        return AgencyInvitation.builder()
                .proposedRole(dto.getProposedRole())
                .message(dto.getMessage())
                .build();
    }

    public AgencyInvitationResponseDto toResponseDto(AgencyInvitation invitation) {
        if (invitation == null) {
            return null;
        }

        return AgencyInvitationResponseDto.builder()
                .id(invitation.getId())
                .agencyId(invitation.getAgency() != null ? invitation.getAgency().getId() : null)
                .receiverId(invitation.getReceiver() != null ? invitation.getReceiver().getId() : null)
                .senderId(invitation.getSender() != null ? invitation.getSender().getId() : null)
                .proposedRole(invitation.getProposedRole())
                .status(invitation.getStatus())
                .sentAt(invitation.getSentAt())
                .respondedAt(invitation.getRespondedAt())
                .message(invitation.getMessage())
                .agencyName(invitation.getAgency() != null ? invitation.getAgency().getName() : null)
                .senderName(invitation.getSender() != null ? invitation.getSender().getFirstName() + " " + invitation.getSender().getLastName() : null)
                .receiverName(invitation.getReceiver() != null ? invitation.getReceiver().getFirstName() + " " + invitation.getReceiver().getLastName() : null)
                .receiverEmail(invitation.getReceiver() != null ? invitation.getReceiver().getEmail() : null)
                .build();
    }
}