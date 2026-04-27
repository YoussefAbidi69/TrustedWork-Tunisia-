package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.AgencyJoinRequest;
import tn.esprit.userservice.entity.JoinRequestStatus;

import java.util.List;

public interface IAgencyJoinRequestService {

    /** Freelancer sends a join request to an agency */
    AgencyJoinRequest sendJoinRequest(Long agencyId, Long requesterId, String message);

    /** Owner retrieves all join requests for their agency (optionally filtered by status) */
    List<AgencyJoinRequest> getRequestsByAgency(Long agencyId, Long requestingOwnerId, JoinRequestStatus status);

    /** Owner accepts or declines a join request */
    AgencyJoinRequest respondToRequest(Long requestId, Long requestingOwnerId, JoinRequestStatus newStatus);

    /** Freelancer cancels their own pending request */
    void cancelRequest(Long requestId, Long requesterId);

    /** Get all requests made by a specific freelancer */
    List<AgencyJoinRequest> getRequestsByUser(Long requesterId);
}
