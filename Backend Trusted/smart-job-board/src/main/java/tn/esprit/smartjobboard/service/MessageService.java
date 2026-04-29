package tn.esprit.smartjobboard.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.ConversationSummaryDto;
import tn.esprit.smartjobboard.dto.MessageDto;
import tn.esprit.smartjobboard.dto.ScheduleMeetRequest;
import tn.esprit.smartjobboard.dto.ScheduleMeetResponse;
import tn.esprit.smartjobboard.dto.SendMessageRequest;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.Message;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MessageRepository;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final JobOfferRepository jobOfferRepository;
    private final FreelancerProfileClient freelancerProfileClient;
    private final CurrentUserService currentUserService;
    private final GoogleMeetService googleMeetService;

    @Transactional
    public MessageDto sendLegacy(Long senderId, SendMessageRequest req) {
        if (req.getJobOfferId() == null || req.getReceiverId() == null) {
            throw new IllegalArgumentException("jobOfferId and receiverId are required.");
        }
        String conversationId = conversationId(req.getJobOfferId(), senderId, req.getReceiverId());
        return send(conversationId, senderId, req);
    }

    @Transactional
    public MessageDto send(String conversationId, Long senderId, SendMessageRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        Message latest = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId).orElse(null);
        Long receiverId;
        Long jobOfferId;
        if (latest != null) {
            if (!senderId.equals(latest.getSenderId()) && !senderId.equals(latest.getReceiverId())) {
                throw new IllegalArgumentException("Unauthorized conversation.");
            }
            receiverId = senderId.equals(latest.getSenderId()) ? latest.getReceiverId() : latest.getSenderId();
            jobOfferId = latest.getJobOfferId();
        } else {
            if (req.getReceiverId() == null || req.getJobOfferId() == null) {
                throw new IllegalArgumentException("receiverId and jobOfferId are required for a new conversation.");
            }
            receiverId = req.getReceiverId();
            jobOfferId = req.getJobOfferId();
            String expected = conversationId(jobOfferId, senderId, receiverId);
            if (!expected.equals(conversationId)) {
                throw new IllegalArgumentException("Invalid conversation id.");
            }
            jobOfferRepository.findById(jobOfferId)
                    .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + jobOfferId));
        }

        Message msg = Message.builder()
                .conversationId(conversationId)
                .jobOfferId(jobOfferId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(req.getContent().trim())
                .type(req.getType() == null ? "text" : req.getType())
                .fileUrl(req.getFileUrl())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(msg);
        return toDto(saved);
    }

    @Transactional
    public List<MessageDto> getConversation(String conversationId, Long currentUserId) {
        Message latest = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found."));
        if (!currentUserId.equals(latest.getSenderId()) && !currentUserId.equals(latest.getReceiverId())) {
            throw new IllegalArgumentException("Unauthorized conversation.");
        }
        Long peerId = currentUserId.equals(latest.getSenderId()) ? latest.getReceiverId() : latest.getSenderId();
        messageRepository.markConversationRead(conversationId, peerId, currentUserId);
        List<Message> messages = messageRepository.findConversation(conversationId);
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public List<MessageDto> getConversationByJobAndPeer(Long jobId, Long currentUserId, Long peerId) {
        String conversationId = conversationId(jobId, currentUserId, peerId);
        return getConversation(conversationId, currentUserId);
    }

    @Transactional
    public ScheduleMeetResponse scheduleMeet(Long currentUserId, ScheduleMeetRequest req) {
        Message latest = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(req.getConversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found."));
        if (!currentUserId.equals(latest.getSenderId()) && !currentUserId.equals(latest.getReceiverId())) {
            throw new IllegalArgumentException("Unauthorized conversation.");
        }
        Long otherPartyId = currentUserId.equals(latest.getSenderId()) ? latest.getReceiverId() : latest.getSenderId();
        String currentEmail = currentUserService.requireCurrentUser().getEmail();
        String otherEmail = Optional.ofNullable(freelancerProfileClient.fetchFreelancerProfile(otherPartyId))
                .map(tn.esprit.smartjobboard.dto.UserReferenceDto::getEmail)
                .orElseThrow(() -> new EntityNotFoundException("Other party email unavailable"));

        java.time.LocalDateTime start = java.time.LocalDateTime.parse(req.getDate() + "T" + req.getTime());
        GoogleMeetService.ScheduleResult meet = googleMeetService.createMeet(
                req.getTitle(),
                req.getNote(),
                start,
                req.getDuration(),
                currentEmail,
                otherEmail
        );

        Message meetMessage = Message.builder()
                .conversationId(req.getConversationId())
                .jobOfferId(latest.getJobOfferId())
                .senderId(currentUserId)
                .receiverId(otherPartyId)
                .content(req.getTitle())
                .type("meet")
                .meetUrl(meet.meetUrl())
                .isRead(false)
                .build();
        messageRepository.save(meetMessage);

        return ScheduleMeetResponse.builder()
                .meetUrl(meet.meetUrl())
                .eventId(meet.eventId())
                .build();
    }

    @Transactional
    public List<ConversationSummaryDto> getConversations(Long currentUserId) {
        List<Message> allMessages = messageRepository.findAllForUser(currentUserId);

        // Group by conversation + peer
        Map<String, List<Message>> grouped = new LinkedHashMap<>();
        for (Message m : allMessages) {
            Long peerId = m.getSenderId().equals(currentUserId) ? m.getReceiverId() : m.getSenderId();
            String key = m.getConversationId() + ":" + peerId;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }

        List<ConversationSummaryDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Message>> entry : grouped.entrySet()) {
            List<Message> thread = entry.getValue();
            Message latest = thread.get(0); // already sorted DESC
            Long peerId = latest.getSenderId().equals(currentUserId) ? latest.getReceiverId() : latest.getSenderId();
            String stableConversationId = latest.getConversationId();
            if (stableConversationId == null || stableConversationId.isBlank()) {
                stableConversationId = conversationId(latest.getJobOfferId(), currentUserId, peerId);
                messageRepository.backfillConversationId(
                        stableConversationId,
                        latest.getJobOfferId(),
                        currentUserId,
                        peerId
                );
            }
            long unread = thread.stream()
                    .filter(m -> m.getReceiverId().equals(currentUserId) && !m.isRead())
                    .count();

            String jobTitle = jobOfferRepository.findById(latest.getJobOfferId())
                    .map(JobOffer::getTitle)
                    .orElse("Job #" + latest.getJobOfferId());

            String peerName = "Freelancer";
            try {
                tn.esprit.smartjobboard.dto.UserReferenceDto peer = freelancerProfileClient.fetchFreelancerProfile(peerId);
                if (peer != null && peer.getFirstName() != null) {
                    peerName = peer.getFirstName() + " " + peer.getLastName();
                }
            } catch (Exception e) {
                // fallback
            }

            result.add(ConversationSummaryDto.builder()
                    .id(stableConversationId)
                    .otherPartyId(toPublicRef(peerId))
                    .otherPartyName(peerName)
                    .jobTitle(jobTitle)
                    .lastMessage(latest.getContent().length() > 80
                            ? latest.getContent().substring(0, 80) + "…"
                            : latest.getContent())
                    .lastMessageAt(latest.getCreatedAt())
                    .unreadCount(unread)
                    .jobOfferId(latest.getJobOfferId())
                    .peerId(peerId)
                    .peerName(peerName)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    private MessageDto toDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .senderId(toPublicRef(m.getSenderId()))
                .content(m.getContent())
                .type(m.getType())
                .fileUrl(m.getFileUrl())
                .meetUrl(m.getMeetUrl())
                .createdAt(m.getCreatedAt())
                .sentAt(m.getCreatedAt())
                .read(m.isRead())
                .build();
    }

    private String toPublicRef(Long userId) {
        return "u_" + Long.toHexString(userId * 2654435761L);
    }

    public String conversationId(Long jobOfferId, Long userA, Long userB) {
        long first = Math.min(userA, userB);
        long second = Math.max(userA, userB);
        String raw = jobOfferId + ":" + first + ":" + second;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                hex.append(String.format("%02x", hashed[i]));
            }
            return "c_" + hex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate conversation id", ex);
        }
    }
}
