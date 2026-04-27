package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.AgencyJoinRequest;
import tn.esprit.userservice.entity.JoinRequestStatus;

import java.util.List;
import java.util.Optional;

public interface IAgencyJoinRequestRepository extends JpaRepository<AgencyJoinRequest, Long> {

    List<AgencyJoinRequest> findByAgencyId(Long agencyId);

    List<AgencyJoinRequest> findByAgencyIdAndStatus(Long agencyId, JoinRequestStatus status);

    List<AgencyJoinRequest> findByRequesterId(Long requesterId);

    Optional<AgencyJoinRequest> findByAgencyIdAndRequesterId(Long agencyId, Long requesterId);

    boolean existsByAgencyIdAndRequesterIdAndStatus(Long agencyId, Long requesterId, JoinRequestStatus status);

    void deleteByAgencyIdAndRequesterId(Long agencyId, Long requesterId);
}
