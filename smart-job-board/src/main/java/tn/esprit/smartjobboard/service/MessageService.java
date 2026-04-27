package tn.esprit.smartjobboard.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.ConversationSummaryDto;
import tn.esprit.smartjobboard.dto.MessageDto;
import tn.esprit.smartjobboard.dto.SendMessageRequest;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.Message;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MessageRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRestClient userRestClient;

    @Transactional
    public MessageDto send(Long senderId, SendMessageRequest req) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty.");
        }
        if (senderId.equals(req.getReceiverId())) {
            throw new IllegalArgumentException("You cannot message yourself.");
        }

        // Verify the job exists
        jobOfferRepository.findById(req.getJobOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Job offer not found: " + req.getJobOfferId()));

        Message msg = Message.builder()
                .jobOfferId(req.getJobOfferId())
                .senderId(senderId)
                .receiverId(req.getReceiverId())
                .content(req.getContent().trim())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(msg);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getConversation(Long jobId, Long currentUserId, Long peerId) {
        List<Message> messages = messageRepository.findConversation(jobId, currentUserId, peerId);
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public int markRead(Long jobId, Long peerId, Long currentUserId) {
        return messageRepository.markConversationRead(jobId, peerId, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations(Long currentUserId) {
        List<Message> allMessages = messageRepository.findAllForUser(currentUserId);

        // Group by (jobOfferId, peerId)
        Map<String, List<Message>> grouped = new LinkedHashMap<>();
        for (Message m : allMessages) {
            Long peerId = m.getSenderId().equals(currentUserId) ? m.getReceiverId() : m.getSenderId();
            String key = m.getJobOfferId() + ":" + peerId;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }

        List<ConversationSummaryDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Message>> entry : grouped.entrySet()) {
            List<Message> thread = entry.getValue();
            Message latest = thread.get(0); // already sorted DESC
            Long peerId = latest.getSenderId().equals(currentUserId) ? latest.getReceiverId() : latest.getSenderId();

            long unread = thread.stream()
                    .filter(m -> m.getReceiverId().equals(currentUserId) && !m.isRead())
                    .count();

            String jobTitle = jobOfferRepository.findById(latest.getJobOfferId())
                    .map(JobOffer::getTitle)
                    .orElse("Job #" + latest.getJobOfferId());

            String peerName = "Peer #" + peerId;
            try {
                tn.esprit.smartjobboard.dto.UserReferenceDto peer = userRestClient.fetchPublicUser(peerId);
                if (peer != null && peer.getFirstName() != null && peer.getLastName() != null) {
                    peerName = peer.getFirstName() + " " + peer.getLastName();
                }
            } catch (Exception e) {
                // Ignore and use default Peer # ID
            }

            result.add(ConversationSummaryDto.builder()
                    .jobOfferId(latest.getJobOfferId())
                    .jobTitle(jobTitle)
                    .peerId(peerId)
                    .peerName(peerName)
                    .lastMessage(latest.getContent().length() > 80
                            ? latest.getContent().substring(0, 80) + "…"
                            : latest.getContent())
                    .lastMessageAt(latest.getSentAt())
                    .unreadCount(unread)
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
                .jobOfferId(m.getJobOfferId())
                .senderId(m.getSenderId())
                .receiverId(m.getReceiverId())
                .content(m.getContent())
                .read(m.isRead())
                .sentAt(m.getSentAt())
                .build();
    }
}
