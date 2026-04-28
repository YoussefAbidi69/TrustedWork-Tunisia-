package tn.esprit.userservice.controller.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import tn.esprit.userservice.dto.chat.ChatMessageDTO;
import tn.esprit.userservice.dto.chat.SendMessageDTO;
import tn.esprit.userservice.dto.chat.TypingEventDTO;
import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.CollaborationLog;
import tn.esprit.userservice.entity.MemberRole;
import tn.esprit.userservice.entity.MemberStatus;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.ICollaborationLogRepository;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.dto.chat.ReactDTO;
import tn.esprit.userservice.dto.chat.ReactionEventDTO;
import tn.esprit.userservice.dto.chat.ReactionSummaryDTO;
import tn.esprit.userservice.dto.chat.AttachmentRefDTO;
import tn.esprit.userservice.entity.MessageAttachment;
import tn.esprit.userservice.repository.MessageAttachmentRepository;
import tn.esprit.userservice.service.MessageReactionService;
import tn.esprit.userservice.service.chat.AgencyChatService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@Controller
@RequiredArgsConstructor
public class AgencyChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final IAgencyRepository agencyRepository;
    private final ICollaborationLogRepository collaborationLogRepository;
    private final IAgencyMemberRepository agencyMemberRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageReactionService reactionService;
    private final AgencyChatService chatService;

    @MessageMapping("/agency/{agencyId}/send")
    public void sendMessage(
            @DestinationVariable Long agencyId,
            @Payload @Valid SendMessageDTO dto,
            Principal principal
    ) {
        String email = principal.getName();
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        AgencyMember am = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, sender.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member"));

        if (am.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Member not active");
        }

        if (am.getRole() == MemberRole.OBSERVER) {
            throw new AccessDeniedException("OBSERVER cannot send messages");
        }

        String safeMsg = dto.getMessage() != null ? dto.getMessage().replaceAll("<[^>]*>", "").trim() : "";
        if (safeMsg.isEmpty() && (dto.getAttachments() == null || dto.getAttachments().isEmpty())) {
            return;
        }

        CollaborationLog log = new CollaborationLog();
        log.setMessage(safeMsg);
        log.setSentAt(LocalDateTime.now());
        log.setSender(sender);
        log.setAgency(agencyRepository.getReferenceById(agencyId));
        
        if (dto.getReplyToId() != null) {
            CollaborationLog parent = collaborationLogRepository.findById(dto.getReplyToId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent message not found"));
            if (!parent.getAgency().getId().equals(agencyId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reply to message from different agency");
            }
            log.setReplyTo(parent);
        }
        if (dto.getTaskRefId() != null) {
            log.setTaskRefId(dto.getTaskRefId());
        }

        CollaborationLog saved = collaborationLogRepository.save(log);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            for (AttachmentRefDTO ref : dto.getAttachments()) {
                MessageAttachment att = new MessageAttachment();
                att.setMessage(saved);
                att.setUrl(ref.getUrl());
                att.setFilename(ref.getFilename());
                att.setFileType(ref.getFileType());
                att.setFileSize(ref.getFileSize());
                att.setUploadedAt(LocalDateTime.now());
                messageAttachmentRepository.save(att);
            }
        }

        ChatMessageDTO chatMessageDTO = chatService.mapToDTO(saved);

        messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/messages", chatMessageDTO);
    }

    @MessageMapping("/agency/{agencyId}/typing")
    public void handleTyping(
            @DestinationVariable Long agencyId,
            @Payload TypingEventDTO dto,
            Principal principal
    ) {
        String email = principal.getName();
        User sender = userRepository.findByEmail(email).orElse(null);
        
        if (sender != null) {
            boolean isActiveMember = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, sender.getId())
                    .filter(am -> am.getStatus() == MemberStatus.ACTIVE)
                    .isPresent();
            
            if (isActiveMember) {
                dto.setAgencyId(agencyId);
                dto.setUserId(sender.getId());
                dto.setFirstName(sender.getFirstName());
                
                messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/typing", dto);
            }
        }
    }

    @MessageMapping("/agency/{agencyId}/react")
    public void handleReaction(
            @DestinationVariable Long agencyId,
            @Payload ReactDTO dto,
            Principal principal
    ) {
        String email = principal.getName();
        User sender = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("User not found"));
        Long userId = sender.getId();

        AgencyMember am = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, userId)
                .orElseThrow(() -> new AccessDeniedException("Not a member"));
        if (am.getStatus() != MemberStatus.ACTIVE) throw new AccessDeniedException("Member not active");

        List<String> validEmojis = Arrays.asList("THUMBS_UP", "CHECK", "EYES", "FIRE", "QUESTION", "PARTY");
        if (!validEmojis.contains(dto.getEmoji())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid emoji");
        }

        reactionService.toggleReaction(dto.getMessageId(), userId, dto.getEmoji(), dto.isRemove());

        List<ReactionSummaryDTO> summary = reactionService.buildReactionSummary(dto.getMessageId());

        ReactionEventDTO event = new ReactionEventDTO(dto.getMessageId(), dto.getEmoji(), userId, dto.isRemove(), summary);
        messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/reactions", event);
    }
}
