package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyInvitationRequestDto;
import tn.esprit.userservice.dto.AgencyInvitationResponseDto;
import tn.esprit.userservice.dto.AgencyInvitationUpdateDto;
import tn.esprit.userservice.entity.AgencyInvitation;
import tn.esprit.userservice.mapper.AgencyInvitationMapper;
import tn.esprit.userservice.service.IAgencyInvitationServices;

import java.util.List;

@RestController
@RequestMapping("/agency-invitations")
@RequiredArgsConstructor
public class AgencyInvitationController {

    private final IAgencyInvitationServices invitationService;
    private final AgencyInvitationMapper agencyInvitationMapper;

    // SEND INVITATION
    @PostMapping("/agency/{agencyId}")
    public AgencyInvitationResponseDto sendInvitation(
            @PathVariable Long agencyId,
            @RequestBody AgencyInvitationRequestDto dto
    ) {
        AgencyInvitation invitation = agencyInvitationMapper.toEntity(dto);
        // Explicitly passing senderId and receiverId from DTO
        AgencyInvitation savedInvitation = invitationService.createInvitation(
                agencyId, 
                dto.getSenderId(), 
                dto.getReceiverId(), 
                invitation
        );
        return agencyInvitationMapper.toResponseDto(savedInvitation);
    }

    // GET INVITATIONS BY AGENCY
    @GetMapping("/agency/{agencyId}")
    public List<AgencyInvitationResponseDto> getInvitationsByAgency(@PathVariable Long agencyId) {
        return invitationService.getInvitationsByAgency(agencyId)
                .stream()
                .map(agencyInvitationMapper::toResponseDto)
                .toList();
    }

    // GET INVITATIONS BY USER
    @GetMapping("/user/{userId}")
    public List<AgencyInvitationResponseDto> getInvitationsByUser(@PathVariable Long userId) {
        return invitationService.getInvitationsByUser(userId)
                .stream()
                .map(agencyInvitationMapper::toResponseDto)
                .toList();
    }

    // RESPOND TO INVITATION (ACCEPT / DECLINE)
    @PutMapping("/{invitationId}/status")
    public AgencyInvitationResponseDto respondToInvitation(
            @PathVariable Long invitationId,
            @RequestBody AgencyInvitationUpdateDto dto
    ) {
        AgencyInvitation updatedInvitation =
                invitationService.updateInvitationStatus(invitationId, dto.getStatus());

        return agencyInvitationMapper.toResponseDto(updatedInvitation);
    }

    // DELETE INVITATION
    @DeleteMapping("/{invitationId}")
    public void deleteInvitation(@PathVariable Long invitationId) {
        invitationService.deleteInvitation(invitationId);
    }
}