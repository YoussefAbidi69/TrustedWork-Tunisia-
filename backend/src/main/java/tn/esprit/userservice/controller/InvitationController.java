package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyInvitationResponseDto;
import tn.esprit.userservice.entity.InvitationStatus;
import tn.esprit.userservice.mapper.AgencyInvitationMapper;
import tn.esprit.userservice.service.IAgencyInvitationServices;

import java.util.List;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final IAgencyInvitationServices invitationService;
    private final AgencyInvitationMapper agencyInvitationMapper;

    // GET ALL INCOMING (RECEIVED) INVITATIONS
    @GetMapping("/received")
    public List<AgencyInvitationResponseDto> getReceivedInvitations(@RequestParam Long userId) {
        return invitationService.getInvitationsByUser(userId)
                .stream()
                .map(agencyInvitationMapper::toResponseDto)
                .toList();
    }

    // ACCEPT INVITATION
    @PatchMapping("/{invitationId}/accept")
    public AgencyInvitationResponseDto acceptInvitation(@PathVariable Long invitationId) {
        tn.esprit.userservice.entity.AgencyInvitation updatedInvitation =
                invitationService.updateInvitationStatus(invitationId, InvitationStatus.ACCEPTED);
        return agencyInvitationMapper.toResponseDto(updatedInvitation);
    }

    // DECLINE INVITATION
    @PatchMapping("/{invitationId}/decline")
    public AgencyInvitationResponseDto declineInvitation(@PathVariable Long invitationId) {
        tn.esprit.userservice.entity.AgencyInvitation updatedInvitation =
                invitationService.updateInvitationStatus(invitationId, InvitationStatus.DECLINED);
        return agencyInvitationMapper.toResponseDto(updatedInvitation);
    }
}
