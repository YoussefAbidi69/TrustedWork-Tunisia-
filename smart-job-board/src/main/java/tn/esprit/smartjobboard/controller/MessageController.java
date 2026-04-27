package tn.esprit.smartjobboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartjobboard.dto.ConversationSummaryDto;
import tn.esprit.smartjobboard.dto.MessageDto;
import tn.esprit.smartjobboard.dto.SendMessageRequest;
import tn.esprit.smartjobboard.service.CurrentUserService;
import tn.esprit.smartjobboard.service.MessageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messaging", description = "In-app messaging between clients and freelancers")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Send a message")
    public ResponseEntity<MessageDto> send(@RequestBody SendMessageRequest req) {
        Long senderId = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.send(senderId, req));
    }

    @GetMapping
    @Operation(summary = "Get conversation messages for a specific job + peer")
    public ResponseEntity<List<MessageDto>> getConversation(
            @RequestParam Long jobId,
            @RequestParam Long peerId
    ) {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.getConversation(jobId, me, peerId));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List all my conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations() {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.getConversations(me));
    }

    @PutMapping("/read")
    @Operation(summary = "Mark messages from a peer as read")
    public ResponseEntity<Map<String, Integer>> markRead(
            @RequestParam Long jobId,
            @RequestParam Long peerId
    ) {
        Long me = currentUserService.requireCurrentUser().getId();
        int count = messageService.markRead(jobId, peerId, me);
        return ResponseEntity.ok(Map.of("markedRead", count));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread message count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(Map.of("unread", messageService.unreadCount(me)));
    }
}
