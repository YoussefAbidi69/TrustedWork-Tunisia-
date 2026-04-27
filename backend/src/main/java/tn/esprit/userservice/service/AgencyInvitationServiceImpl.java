package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.IAgencyInvitationRepository;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyInvitationServiceImpl implements IAgencyInvitationServices {

    private final IAgencyInvitationRepository invitationRepository;
    private final IAgencyRepository agencyRepository;
    private final IAgencyMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    public AgencyInvitation createInvitation(Long agencyId, Long senderId, Long receiverId, AgencyInvitation invitation) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender Lead not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver User not found"));

        // Verify sender is a LEAD in this agency
        AgencyMember senderMembership = memberRepository
                .findByAgencyIdAndUserId(agencyId, senderId)
                .orElseThrow(() -> new RuntimeException("Sender is not a member of this agency"));

        if (senderMembership.getRole() != MemberRole.LEAD) {
            throw new RuntimeException("Only a LEAD can send invitations");
        }

        if (invitationRepository.findByAgencyIdAndReceiverIdAndStatus(agencyId, receiverId, InvitationStatus.PENDING).isPresent()) {
            throw new RuntimeException("This user already has a pending invitation for this agency");
        }

        // Check if already a member
        if (memberRepository.existsByAgencyIdAndUserId(agencyId, receiverId)) {
            throw new RuntimeException("User is already a member of this agency");
        }

        invitation.setAgency(agency);
        invitation.setSender(sender);
        invitation.setReceiver(receiver);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setSentAt(LocalDateTime.now());
        if (invitation.getProposedRole() == null) {
            invitation.setProposedRole(MemberRole.MEMBER);
        }

        // Notification: Invitee receives an in-app notification when a LEAD sends them an invitation
        System.out.println("[NOTIFICATION] To User ID (" + receiverId + "): Vous avez reçu une invitation de l'agence ID (" + agencyId + ") envoyée par le LEAD ID (" + senderId + ").");

        return invitationRepository.save(invitation);
    }

    @Override
    public List<AgencyInvitation> getInvitationsByAgency(Long agencyId) {
        return invitationRepository.findByAgencyId(agencyId);
    }

    @Override
    public List<AgencyInvitation> getInvitationsByUser(Long userId) {
        return invitationRepository.findByReceiverId(userId);
    }

    @Override
    public List<AgencyInvitation> getInvitationsByUserAndStatus(Long userId, InvitationStatus status) {
        return invitationRepository.findByReceiverIdAndStatus(userId, status);
    }

    @Override
    @Transactional
    public AgencyInvitation updateInvitationStatus(Long invitationId, InvitationStatus status) {
        AgencyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Can only respond to pending invitations");
        }

        invitation.setStatus(status);
        invitation.setRespondedAt(LocalDateTime.now());

        // ── Business Logic: Acceptance Flow ──────────────────────────────────
        if (status == InvitationStatus.ACCEPTED) {
            AgencyMember newMember = AgencyMember.builder()
                    .agency(invitation.getAgency())
                    .user(invitation.getReceiver())
                    .role(invitation.getProposedRole()) // Usually MEMBER
                    .status(MemberStatus.ACTIVE)
                    .workloadScore(0f)
                    .build();
            
            memberRepository.save(newMember);
        }

        // Notification: LEAD receives an in-app notification when their invitation is accepted or declined
        System.out.println("[NOTIFICATION] To LEAD ID (" + invitation.getSender().getId() + "): L'utilisateur ID (" + invitation.getReceiver().getId() + ") a " + (status == InvitationStatus.ACCEPTED ? "accepté" : "décliné") + " votre invitation pour rejoindre l'agence ID (" + invitation.getAgency().getId() + ").");

        invitationRepository.delete(invitation);
        return invitation;
    }

    @Override
    public AgencyInvitation getInvitationById(Long invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
    }

    @Override
    @Transactional
    public void deleteInvitation(Long invitationId) {
        // Enforce setting status to CANCELLED for pending invitations
        AgencyInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new RuntimeException("Can only cancel pending invitations");
        }
        invitationRepository.delete(invitation);
    }
}
