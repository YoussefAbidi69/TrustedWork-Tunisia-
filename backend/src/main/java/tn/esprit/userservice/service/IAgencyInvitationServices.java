package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.AgencyInvitation;
import tn.esprit.userservice.entity.InvitationStatus;
import java.util.List;

public interface IAgencyInvitationServices {

    AgencyInvitation createInvitation(Long agencyId, Long senderId, Long receiverId, AgencyInvitation invitation);

    List<AgencyInvitation> getInvitationsByAgency(Long agencyId);

    List<AgencyInvitation> getInvitationsByUser(Long userId);

    List<AgencyInvitation> getInvitationsByUserAndStatus(Long userId, InvitationStatus status);

    AgencyInvitation updateInvitationStatus(Long invitationId, InvitationStatus status);

    AgencyInvitation getInvitationById(Long invitationId);

    void deleteInvitation(Long invitationId);
}