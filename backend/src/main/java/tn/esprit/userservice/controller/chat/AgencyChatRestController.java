package tn.esprit.userservice.controller.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.chat.ChatMessageDTO;
import tn.esprit.userservice.dto.chat.MessageHistoryDTO;
import tn.esprit.userservice.dto.chat.OnlineMemberDTO;
import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.CollaborationLog;
import tn.esprit.userservice.entity.MemberRole;
import tn.esprit.userservice.entity.MemberStatus;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.ICollaborationLogRepository;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.service.chat.AgencyChatService;

import tn.esprit.userservice.repository.ITaskRepository;
import tn.esprit.userservice.repository.MessageAttachmentRepository;
import tn.esprit.userservice.repository.MessageReactionRepository;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.dto.chat.TaskCardDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agencies/{agencyId}/chat")
@RequiredArgsConstructor
public class AgencyChatRestController {

    private final AgencyChatService chatService;
    private final IAgencyMemberRepository agencyMemberRepository;
    private final UserRepository userRepository;
    private final ICollaborationLogRepository collaborationLogRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ITaskRepository taskRepository;

    @GetMapping("/messages")
    public ResponseEntity<Page<MessageHistoryDTO>> getMessageHistory(
            @PathVariable Long agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        validateActiveMember(agencyId, authentication);
        return ResponseEntity.ok(chatService.getMessageHistory(agencyId, PageRequest.of(page, size)));
    }

    @GetMapping("/members/online")
    public ResponseEntity<Map<String, Object>> getOnlineMembers(
            @PathVariable Long agencyId,
            Authentication authentication
    ) {
        validateActiveMember(agencyId, authentication);
        List<OnlineMemberDTO> members = chatService.getOnlineMembersList(agencyId);
        long onlineCount = members.stream().filter(OnlineMemberDTO::isOnline).count();
        
        return ResponseEntity.ok(Map.of(
                "totalMembers", members.size(),
                "onlineCount", onlineCount,
                "members", members
        ));
    }

    @GetMapping("/tasks/search")
    public ResponseEntity<List<TaskCardDTO>> searchTasks(
            @PathVariable Long agencyId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication
    ) {
        validateActiveMember(agencyId, authentication);
        Page<Task> page = taskRepository.searchByAgencyAndTitle(agencyId, q, PageRequest.of(0, limit));
        List<TaskCardDTO> res = page.getContent().stream().map(t -> {
            TaskCardDTO dto = new TaskCardDTO();
            dto.setId(t.getId());
            dto.setTitle(t.getTitle());
            dto.setStatus(t.getStatus().name());
            dto.setPriority(t.getPriority().name());
            if (t.getAssignedMember() != null && t.getAssignedMember().getUser() != null) {
                dto.setAssigneeName(t.getAssignedMember().getUser().getFirstName() + " " + t.getAssignedMember().getUser().getLastName());
                dto.setAssigneePhoto(t.getAssignedMember().getUser().getPhoto());
            }
            if (t.getDueDate() != null) {
                dto.setDueDate(t.getDueDate().toString());
            }
            if (t.getProject() != null) {
                dto.setProjectTitle(t.getProject().getName());
            }
            int progress = "TERMINE".equals(dto.getStatus()) ? 100 : ("EN_COURS".equals(dto.getStatus()) ? 50 : 0);
            dto.setProgressPercent(progress);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/messages/pinned")
    public ResponseEntity<List<ChatMessageDTO>> getPinnedMessages(
            @PathVariable Long agencyId,
            Authentication authentication
    ) {
        validateActiveMember(agencyId, authentication);
        List<CollaborationLog> logs = collaborationLogRepository.findPinnedMessagesByAgencyId(agencyId);
        List<ChatMessageDTO> dtos = logs.stream().map(chatService::mapToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/messages/{messageId}/pin")
    public ResponseEntity<Void> pinMessage(
            @PathVariable Long agencyId,
            @PathVariable Long messageId,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication
    ) {
        User user = validateActiveMember(agencyId, authentication);
        AgencyMember am = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, user.getId()).get();
        if (am.getRole() != MemberRole.LEAD) {
            return ResponseEntity.status(403).build();
        }

        CollaborationLog log = collaborationLogRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!log.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Message does not belong to this agency");
        }

        boolean pin = body.getOrDefault("pin", true);
        if (pin) {
            List<CollaborationLog> pinned = collaborationLogRepository.findPinnedMessagesByAgencyId(agencyId);
            if (pinned.size() >= 5 && !log.isPinned()) {
                throw new RuntimeException("Maximum 5 pinned messages allowed");
            }
        }

        log.setPinned(pin);
        log.setPinnedAt(pin ? java.time.LocalDateTime.now() : null);
        log.setPinnedById(pin ? user.getId() : null);
        collaborationLogRepository.save(log);

        ChatMessageDTO chatMessageDTO = chatService.mapToDTO(log);
        messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/messages", chatMessageDTO);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long agencyId,
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        User user = validateActiveMember(agencyId, authentication);
        AgencyMember am = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, user.getId()).get();

        CollaborationLog log = collaborationLogRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!log.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Message does not belong to this agency");
        }

        if (!log.getSender().getId().equals(user.getId()) && am.getRole() != MemberRole.LEAD) {
            return ResponseEntity.status(403).build();
        }

        log.setMessage("[Message supprimé]");
        log.setDeleted(true);
        collaborationLogRepository.save(log);

        ChatMessageDTO chatMessageDTO = chatService.mapToDTO(log);

        messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/messages", chatMessageDTO);

        return ResponseEntity.noContent().build();
    }

    private User validateActiveMember(Long agencyId, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isActiveMember = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, user.getId())
                .filter(am -> am.getStatus() == MemberStatus.ACTIVE)
                .isPresent();

        if (!isActiveMember) {
            throw new RuntimeException("Access denied: Not an active member");
        }
        
        return user;
    }
}
