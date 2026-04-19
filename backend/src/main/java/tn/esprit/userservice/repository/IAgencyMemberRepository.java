package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.MemberRole;
import tn.esprit.userservice.entity.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface IAgencyMemberRepository extends JpaRepository<AgencyMember, Long> {

    List<AgencyMember> findByAgencyId(Long agencyId);

    // Updated to use the new status enum instead of active boolean
    List<AgencyMember> findByAgencyIdAndStatus(Long agencyId, MemberStatus status);

    // Spring Data JPA automatically maps userId to user.id relationship
    Optional<AgencyMember> findByAgencyIdAndUserId(Long agencyId, Long userId);

    boolean existsByAgencyIdAndUserId(Long agencyId, Long userId);

    List<AgencyMember> findByAgencyIdAndRole(Long agencyId, MemberRole role);

    // Added methods for UX flow
    List<AgencyMember> findByUserId(Long userId);

    List<AgencyMember> findByUserIdAndRole(Long userId, MemberRole role);

    List<AgencyMember> findByUserIdAndStatus(Long userId, MemberStatus status);
}