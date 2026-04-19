package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.MemberStatus;
import java.util.List;

public interface IAgencyMemberServices {

    AgencyMember addMember(Long agencyId, Long userId, AgencyMember member); // Added userId

    List<AgencyMember> getMembersByAgency(Long agencyId);

    List<AgencyMember> getMembersByAgencyAndStatus(Long agencyId, MemberStatus status); // Replaced getActive...

    AgencyMember getMemberById(Long memberId);

    void deleteMember(Long memberId);

    AgencyMember updateMember(Long memberId, AgencyMember updatedMember);
}