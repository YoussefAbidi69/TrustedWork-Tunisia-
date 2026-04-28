package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyRequestDto;
import tn.esprit.userservice.dto.AgencyResponseDto;
import tn.esprit.userservice.dto.AgencyUpdateDto;
import tn.esprit.userservice.dto.AgencyContextDto;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.mapper.AgencyMapper;
import tn.esprit.userservice.mapper.UserMapper;
import tn.esprit.userservice.service.IAgencyServices;

import java.util.List;

@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final IAgencyServices agencyService;
    private final tn.esprit.userservice.service.IAgencyMemberServices agencyMemberService;
    private final tn.esprit.userservice.service.IAgencyInvitationServices invitationService;
    private final AgencyMapper agencyMapper;
    private final UserMapper userMapper;
    private final tn.esprit.userservice.mapper.AgencyMemberMapper agencyMemberMapper;
    private final tn.esprit.userservice.mapper.AgencyInvitationMapper agencyInvitationMapper;

    // CREATE
    @PostMapping
    public ResponseEntity<?> createAgency(@RequestBody AgencyRequestDto dto) {
        try {
            System.out.println("[AgencyController] POST /agencies - payload: " + dto);
            Agency agency = agencyMapper.toEntity(dto);
            Agency savedAgency = agencyService.createAgency(agency, dto.getCreatorId());
            AgencyResponseDto responseDto = agencyMapper.toResponseDto(savedAgency);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (Exception e) {
            System.err.println("[AgencyController] Erreur lors de la creation de l'agence: ");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Erreur serveur: " + e.getMessage()));
        }
    }

    // GET ALL
    @GetMapping
    public List<AgencyResponseDto> getAllAgencies(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long userId) {
        if ("member".equalsIgnoreCase(role) && userId != null) {
            return agencyService.getMyAgencies(userId)
                    .stream()
                    .map(agencyMapper::toResponseDto)
                    .toList();
        }
        return agencyService.getAllAgencies()
                .stream()
                .map(agencyMapper::toResponseDto)
                .toList();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public AgencyResponseDto getAgencyById(@PathVariable Long id) {
        Agency agency = agencyService.getAgencyById(id);
        return agencyMapper.toResponseDto(agency);
    }

    // GET BY CREATOR (Renamed from owner)
    @GetMapping("/creator/{creatorId}")
    public List<AgencyResponseDto> getAgenciesByCreator(@PathVariable Long creatorId) {
        return agencyService.getAgenciesByCreator(creatorId)
                .stream()
                .map(agencyMapper::toResponseDto)
                .toList();
    }

    // UPDATE
    @PutMapping("/{id}")
    public AgencyResponseDto updateAgency(@PathVariable Long id, @RequestParam Long userId, @RequestBody AgencyUpdateDto dto) {
        Agency existingAgency = agencyService.getAgencyById(id);
        agencyMapper.updateEntityFromDto(dto, existingAgency);
        Agency updatedAgency = agencyService.updateAgency(id, existingAgency, userId);
        return agencyMapper.toResponseDto(updatedAgency);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void deleteAgency(@PathVariable Long id, @RequestParam Long userId) {
        agencyService.deleteAgency(id, userId);
    }

    // GET MY CONTEXT
    @GetMapping("/my-context/{userId}")
    public AgencyContextDto getMyAgencyContext(@PathVariable Long userId) {
        return agencyService.getMyAgencyContext(userId);
    }

    // GET MY AGENCIES
    @GetMapping("/my-agencies/{userId}")
    public List<AgencyResponseDto> getMyAgencies(@PathVariable Long userId) {
        return agencyService.getMyAgencies(userId)
                .stream()
                .map(agencyMapper::toResponseDto)
                .toList();
    }

    // GET AVAILABLE FREELANCERS
    @GetMapping("/{agencyId}/available-freelancers")
    public List<PublicUserDTO> getAvailableFreelancers(
            @PathVariable Long agencyId,
            @RequestParam Long userId,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String search) {
        return agencyService.getAvailableFreelancers(agencyId, userId, skill, search)
                .stream()
                .map(userMapper::toPublicDto)
                .toList();
    }

    // GET ALL MEMBERS OF AN AGENCY
    @GetMapping("/{id}/members")
    public List<tn.esprit.userservice.dto.AgencyMemberResponseDto> getAgencyMembers(@PathVariable Long id) {
        return agencyMemberService.getMembersByAgency(id)
                .stream()
                .map(agencyMemberMapper::toResponseDto)
                .toList();
    }

    // REMOVE MEMBER
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam Long ownerId) {
        try {
            agencyMemberService.removeMember(id, userId, ownerId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/quit")
    public ResponseEntity<?> quitAgency(
            @PathVariable Long id,
            @RequestParam Long userId) {
        try {
            agencyMemberService.quitAgency(id, userId);
            return ResponseEntity.ok().body(java.util.Map.of("message", "You have successfully left the agency"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    // UPDATE MEMBER ROLE
    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody java.util.Map<String, String> body,
            @RequestParam Long requesterId) {
        try {
            // Require LEAD
            AgencyContextDto context = agencyService.getMyAgencyContext(requesterId);
            boolean isLead = context.getMemberships().stream()
                .anyMatch(m -> m.getAgencyId().equals(id) && "LEAD".equals(m.getRole()));
            if (!isLead) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only LEAD can update roles");
            }
            
            tn.esprit.userservice.entity.AgencyMember existingMember = agencyMemberService.getMembersByAgency(id)
                .stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Member not found"));

            if (body.containsKey("role")) {
                existingMember.setRole(tn.esprit.userservice.entity.MemberRole.valueOf(body.get("role")));
                agencyMemberService.updateMember(existingMember.getId(), existingMember);
            }
            
            return ResponseEntity.ok(agencyMemberMapper.toResponseDto(existingMember));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    // ── INVITATIONS ENDPOINTS ──

    @PostMapping("/{id}/invitations")
    public ResponseEntity<?> sendInvitation(
            @PathVariable Long id,
            @RequestBody tn.esprit.userservice.dto.AgencyInvitationRequestDto dto) {
        try {
            tn.esprit.userservice.entity.AgencyInvitation invitation = new tn.esprit.userservice.entity.AgencyInvitation();
            invitation.setMessage(dto.getMessage());
            invitation.setProposedRole(dto.getProposedRole());
            
            tn.esprit.userservice.entity.AgencyInvitation savedInvitation = invitationService.createInvitation(
                    id, dto.getSenderId(), dto.getReceiverId(), invitation
            );
            return ResponseEntity.ok(agencyInvitationMapper.toResponseDto(savedInvitation));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    // LIST OUTGOING INVITATIONS
    @GetMapping("/{id}/invitations")
    public List<tn.esprit.userservice.dto.AgencyInvitationResponseDto> getOutgoingInvitations(@PathVariable Long id) {
        return invitationService.getInvitationsByAgency(id)
                .stream()
                .map(agencyInvitationMapper::toResponseDto)
                .toList();
    }

    // CANCEL PENDING INVITATION
    @DeleteMapping("/{id}/invitations/{invitationId}")
    public void cancelInvitation(@PathVariable Long id, @PathVariable Long invitationId) {
        invitationService.deleteInvitation(invitationId);
    }

    // GET ANALYTICS
    @GetMapping("/{id}/analytics")
    public tn.esprit.userservice.dto.AgencyAnalyticsDto getAgencyAnalytics(
            @PathVariable Long id,
            @RequestParam Long userId) {
        return agencyService.getAgencyAnalytics(id, userId);
    }
}