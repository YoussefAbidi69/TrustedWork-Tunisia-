package tn.esprit.userservice.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.chat.*;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.*;
import tn.esprit.userservice.service.MessageReactionService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgencyChatService {

    private final IAgencyMemberRepository agencyMemberRepository;
    private final ICollaborationLogRepository collaborationLogRepository;
    private final AgencyPresenceService presenceService;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final MessageReactionService reactionService;
    private final ITaskRepository taskRepository;

    public Page<MessageHistoryDTO> getMessageHistory(Long agencyId, Pageable pageable) {
        Page<CollaborationLog> page = collaborationLogRepository.findByAgencyIdWithReplies(agencyId, pageable);
        List<Long> messageIds = page.getContent().stream().map(CollaborationLog::getId).collect(Collectors.toList());
        Map<Long, List<ReactionSummaryDTO>> reactionsMap = reactionService.loadReactionSummaries(messageIds);

        return page.map(log -> {
            ChatMessageDTO dto = mapToDTO(log);
            dto.setReactions(reactionsMap.getOrDefault(log.getId(), List.of()));
            
            // Map to MessageHistoryDTO manually to maintain compatibility
            return MessageHistoryDTO.builder()
                    .id(dto.getId())
                    .senderId(dto.getSenderId())
                    .senderFirstName(dto.getSenderFirstName())
                    .senderLastName(dto.getSenderLastName())
                    .senderPhoto(dto.getSenderPhoto())
                    .senderRole(dto.getSenderRole())
                    .message(dto.getMessage())
                    .attachmentUrl(null) // Deprecated
                    .sentAt(dto.getSentAt())
                    .deleted(dto.isDeleted())
                    .isPinned(dto.isPinned())
                    .attachments(dto.getAttachments())
                    .taskRef(dto.getTaskRef())
                    .replyTo(dto.getReplyTo())
                    .reactions(dto.getReactions())
                    .build();
        });
    }

    public ChatMessageDTO mapToDTO(CollaborationLog log) {
        User sender = log.getSender();
        String role = "MEMBER";
        if (sender != null) {
            AgencyMember am = agencyMemberRepository.findByAgencyIdAndUserId(log.getAgency().getId(), sender.getId()).orElse(null);
            if (am != null) {
                role = am.getRole().name();
            }
        }
        
        List<MessageAttachment> atts = messageAttachmentRepository.findByMessageId(log.getId());
        List<AttachmentDTO> attDTOs = atts.stream().map(a -> new AttachmentDTO(a.getId(), a.getUrl(), a.getFilename(), a.getFileType(), a.getFileSize())).collect(Collectors.toList());

        ReplyPreviewDTO replyDto = null;
        if (log.getReplyTo() != null) {
            boolean hasAtt = !messageAttachmentRepository.findByMessageId(log.getReplyTo().getId()).isEmpty();
            String prev = log.getReplyTo().getMessage();
            if (prev != null && prev.length() > 100) prev = prev.substring(0, 100);
            replyDto = new ReplyPreviewDTO(
                log.getReplyTo().getId(),
                log.getReplyTo().getSender() != null ? log.getReplyTo().getSender().getFirstName() : "",
                log.getReplyTo().getSender() != null ? log.getReplyTo().getSender().getLastName() : "",
                prev,
                log.getReplyTo().getSentAt() != null ? log.getReplyTo().getSentAt().toString() : null,
                hasAtt
            );
        }

        TaskCardDTO taskDto = null;
        if (log.getTaskRefId() != null) {
            Task task = taskRepository.findById(log.getTaskRefId()).orElse(null);
            if (task != null) {
                taskDto = new TaskCardDTO();
                taskDto.setId(task.getId());
                taskDto.setTitle(task.getTitle());
                taskDto.setStatus(task.getStatus().name());
                taskDto.setPriority(task.getPriority().name());
                if (task.getAssignedMember() != null && task.getAssignedMember().getUser() != null) {
                    taskDto.setAssigneeName(task.getAssignedMember().getUser().getFirstName() + " " + task.getAssignedMember().getUser().getLastName());
                    taskDto.setAssigneePhoto(task.getAssignedMember().getUser().getPhoto());
                }
                if (task.getDueDate() != null) taskDto.setDueDate(task.getDueDate().toString());
                if (task.getProject() != null) taskDto.setProjectTitle(task.getProject().getName());
                taskDto.setProgressPercent("TERMINE".equals(taskDto.getStatus()) ? 100 : ("EN_COURS".equals(taskDto.getStatus()) ? 50 : 0));
            }
        }

        return ChatMessageDTO.builder()
                .id(log.getId())
                .agencyId(log.getAgency().getId())
                .senderId(sender != null ? sender.getId() : null)
                .senderFirstName(sender != null ? sender.getFirstName() : null)
                .senderLastName(sender != null ? sender.getLastName() : null)
                .senderPhoto(sender != null ? sender.getPhoto() : null)
                .senderRole(role)
                .message(log.getMessage())
                .sentAt(log.getSentAt() != null ? log.getSentAt().toString() : null)
                .isDeleted(log.isDeleted())
                .isPinned(log.isPinned())
                .attachments(attDTOs)
                .taskRef(taskDto)
                .replyTo(replyDto)
                .build();
    }

    public List<OnlineMemberDTO> getOnlineMembersList(Long agencyId) {
        List<AgencyMember> activeMembers = agencyMemberRepository.findByAgencyIdAndStatus(agencyId, MemberStatus.ACTIVE);
        
        return activeMembers.stream()
                .map(am -> {
                    User user = am.getUser();
                    boolean isOnline = presenceService.isOnline(agencyId, user.getId());
                    return OnlineMemberDTO.builder()
                            .userId(user.getId())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .photo(user.getPhoto())
                            .role(am.getRole().name())
                            .isOnline(isOnline)
                            .build();
                })
                .sorted(Comparator.comparing((OnlineMemberDTO m) -> m.getRole().equals("LEAD") ? 0 : m.getRole().equals("MEMBER") ? 1 : 2)
                        .thenComparing(m -> !m.isOnline())
                        .thenComparing(OnlineMemberDTO::getFirstName))
                .collect(Collectors.toList());
    }
}
