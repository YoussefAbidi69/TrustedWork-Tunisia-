package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyMemberServiceImpl implements IAgencyMemberServices {

    private final IAgencyMemberRepository agencyMemberRepository;
    private final IAgencyRepository agencyRepository;
    private final UserRepository userRepository;

    @Override
    public AgencyMember addMember(Long agencyId, Long userId, AgencyMember member) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (agencyMemberRepository.existsByAgencyIdAndUserId(agencyId, userId)) {
            throw new RuntimeException("User is already a member of this agency");
        }

        member.setAgency(agency);
        member.setUser(user);
        member.setStatus(MemberStatus.ACTIVE);

        return agencyMemberRepository.save(member);
    }

    @Override
    public List<AgencyMember> getMembersByAgency(Long agencyId) {
        return agencyMemberRepository.findByAgencyId(agencyId);
    }

    @Override
    public List<AgencyMember> getMembersByAgencyAndStatus(Long agencyId, MemberStatus status) {
        return agencyMemberRepository.findByAgencyIdAndStatus(agencyId, status);
    }

    @Override
    public AgencyMember getMemberById(Long memberId) {
        return agencyMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Agency member not found"));
    }

    @Override
    public void deleteMember(Long memberId) {
        agencyMemberRepository.deleteById(memberId);
    }

    @Override
    public AgencyMember updateMember(Long memberId, AgencyMember updatedMember) {
        AgencyMember existingMember = agencyMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Agency member not found"));

        if (updatedMember.getRole() != null) {
            existingMember.setRole(updatedMember.getRole());
        }

        if (updatedMember.getWorkloadScore() != null) {
            existingMember.setWorkloadScore(updatedMember.getWorkloadScore());
        }

        if (updatedMember.getStatus() != null) {
            existingMember.setStatus(updatedMember.getStatus());
        }

        if (updatedMember.getSkills() != null) {
            existingMember.setSkills(updatedMember.getSkills());
        }

        return agencyMemberRepository.save(existingMember);
    }
}