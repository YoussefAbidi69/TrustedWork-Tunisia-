package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.AgencyInvitation;
import tn.esprit.userservice.entity.InvitationStatus;

import java.util.List;
import java.util.Optional;

public interface IAgencyInvitationRepository extends JpaRepository<AgencyInvitation, Long> {

  List<AgencyInvitation> findByAgencyId(Long agencyId);

  List<AgencyInvitation> findByReceiverId(Long userId);

  List<AgencyInvitation> findByReceiverIdAndStatus(Long userId, InvitationStatus status);

  List<AgencyInvitation> findByAgencyIdAndStatus(Long agencyId, InvitationStatus status);

  Optional<AgencyInvitation> findByAgencyIdAndReceiverIdAndStatus(Long agencyId, Long receiverId, InvitationStatus status);

  Optional<AgencyInvitation> findByAgencyIdAndReceiverId(Long agencyId, Long userId);
}