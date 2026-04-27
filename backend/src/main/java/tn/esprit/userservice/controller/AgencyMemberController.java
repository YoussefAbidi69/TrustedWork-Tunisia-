package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyMemberRequestDto;
import tn.esprit.userservice.dto.AgencyMemberResponseDto;
import tn.esprit.userservice.dto.AgencyMemberUpdateDto;
import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.MemberStatus;
import tn.esprit.userservice.mapper.AgencyMemberMapper;
import tn.esprit.userservice.service.IAgencyMemberServices;

import java.util.List;

@RestController
@RequestMapping("/agency-members")
@RequiredArgsConstructor
public class AgencyMemberController {

    private final IAgencyMemberServices agencyMemberService;
    private final AgencyMemberMapper agencyMemberMapper;

    // ADD MEMBER TO AGENCY
    @PostMapping("/agency/{agencyId}")
    public AgencyMemberResponseDto addMember(
            @PathVariable Long agencyId,
            @RequestBody AgencyMemberRequestDto dto
    ) {
        AgencyMember member = agencyMemberMapper.toEntity(dto);
        // Explicitly passing the userId to handle User relationship mapping
        AgencyMember savedMember = agencyMemberService.addMember(agencyId, dto.getUserId(), member);
        return agencyMemberMapper.toResponseDto(savedMember);
    }

    // GET ALL MEMBERS OF AN AGENCY
    @GetMapping("/agency/{agencyId}")
    public List<AgencyMemberResponseDto> getMembersByAgency(@PathVariable Long agencyId) {
        return agencyMemberService.getMembersByAgency(agencyId)
                .stream()
                .map(agencyMemberMapper::toResponseDto)
                .toList();
    }

    // GET ACTIVE MEMBERS OF AN AGENCY
    @GetMapping("/agency/{agencyId}/active")
    public List<AgencyMemberResponseDto> getActiveMembersByAgency(@PathVariable Long agencyId) {
        return agencyMemberService.getMembersByAgencyAndStatus(agencyId, MemberStatus.ACTIVE)
                .stream()
                .map(agencyMemberMapper::toResponseDto)
                .toList();
    }

    // GET MEMBER BY ID
    @GetMapping("/{memberId}")
    public AgencyMemberResponseDto getMemberById(@PathVariable Long memberId) {
        AgencyMember member = agencyMemberService.getMemberById(memberId);
        return agencyMemberMapper.toResponseDto(member);
    }

    // UPDATE MEMBER
    @PutMapping("/{memberId}")
    public AgencyMemberResponseDto updateMember(
            @PathVariable Long memberId,
            @RequestBody AgencyMemberUpdateDto dto
    ) {
        AgencyMember existingMember = agencyMemberService.getMemberById(memberId);
        agencyMemberMapper.updateEntityFromDto(dto, existingMember);
        AgencyMember updatedMember = agencyMemberService.updateMember(memberId, existingMember);
        return agencyMemberMapper.toResponseDto(updatedMember);
    }

    // DELETE MEMBER
    @DeleteMapping("/{memberId}")
    public void deleteMember(@PathVariable Long memberId) {
        agencyMemberService.deleteMember(memberId);
    }
}