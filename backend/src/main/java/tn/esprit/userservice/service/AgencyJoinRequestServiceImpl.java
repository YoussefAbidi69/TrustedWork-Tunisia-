package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgencyJoinRequestServiceImpl implements IAgencyJoinRequestService {

    private final IAgencyJoinRequestRepository joinRequestRepository;
    private final IAgencyRepository agencyRepository;
    private final IAgencyMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AgencyJoinRequest sendJoinRequest(Long agencyId, Long requesterId, String message) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Guard: no duplicate pending request
        if (joinRequestRepository.existsByAgencyIdAndRequesterIdAndStatus(agencyId, requesterId, JoinRequestStatus.PENDING)) {
            throw new RuntimeException("You already have a pending join request for this agency");
        }

        // Guard: not already a member
        if (memberRepository.existsByAgencyIdAndUserId(agencyId, requesterId)) {
            throw new RuntimeException("You are already a member of this agency");
        }

        AgencyJoinRequest request = AgencyJoinRequest.builder()
                .agency(agency)
                .requester(requester)
                .status(JoinRequestStatus.PENDING)
                .message(message)
                .requestedAt(LocalDateTime.now())
                .build();

        return joinRequestRepository.save(request);
    }

    @Override
    public List<AgencyJoinRequest> getRequestsByAgency(Long agencyId, Long requestingOwnerId, JoinRequestStatus status) {
        // Verify owner/lead
        AgencyMember member = memberRepository.findByAgencyIdAndUserId(agencyId, requestingOwnerId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this agency"));
        if (member.getRole() != MemberRole.LEAD) {
            throw new RuntimeException("Only the agency LEAD can view join requests");
        }

        if (status != null) {
            return joinRequestRepository.findByAgencyIdAndStatus(agencyId, status);
        }
        return joinRequestRepository.findByAgencyId(agencyId);
    }

    @Override
    @Transactional
    public AgencyJoinRequest respondToRequest(Long requestId, Long requestingOwnerId, JoinRequestStatus newStatus) {
        AgencyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));

        // Verify responder is LEAD of that agency
        AgencyMember ownerMember = memberRepository.findByAgencyIdAndUserId(request.getAgency().getId(), requestingOwnerId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this agency"));
        if (ownerMember.getRole() != MemberRole.LEAD) {
            throw new RuntimeException("Only the agency LEAD can respond to join requests");
        }

        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new RuntimeException("Can only respond to PENDING requests");
        }

        request.setStatus(newStatus);
        request.setRespondedAt(LocalDateTime.now());

        // If accepted → create AgencyMember
        if (newStatus == JoinRequestStatus.ACCEPTED) {
            boolean alreadyMember = memberRepository.existsByAgencyIdAndUserId(
                    request.getAgency().getId(), request.getRequester().getId());
            if (!alreadyMember) {
                AgencyMember newMember = AgencyMember.builder()
                        .agency(request.getAgency())
                        .user(request.getRequester())
                        .role(MemberRole.MEMBER)
                        .status(MemberStatus.ACTIVE)
                        .workloadScore(0f)
                        .build();
                memberRepository.save(newMember);
            }
        }

        return joinRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void cancelRequest(Long requestId, Long requesterId) {
        AgencyJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));
        if (!request.getRequester().getId().equals(requesterId)) {
            throw new RuntimeException("You can only cancel your own requests");
        }
        if (request.getStatus() != JoinRequestStatus.PENDING) {
            throw new RuntimeException("Only PENDING requests can be cancelled");
        }
        joinRequestRepository.deleteById(requestId);
    }

    @Override
    public List<AgencyJoinRequest> getRequestsByUser(Long requesterId) {
        return joinRequestRepository.findByRequesterId(requesterId);
    }
}
