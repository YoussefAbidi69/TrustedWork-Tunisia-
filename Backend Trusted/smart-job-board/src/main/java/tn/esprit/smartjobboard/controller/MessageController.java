package tn.esprit.smartjobboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.smartjobboard.dto.ConversationSummaryDto;
import tn.esprit.smartjobboard.dto.MessageDto;
import tn.esprit.smartjobboard.dto.ScheduleMeetRequest;
import tn.esprit.smartjobboard.dto.ScheduleMeetResponse;
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

    @PostMapping("/conversations/{id}")
    @Operation(summary = "Send a message")
    public ResponseEntity<MessageDto> send(@PathVariable("id") String conversationId,
                                           @RequestBody SendMessageRequest req) {
        Long senderId = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.send(conversationId, senderId, req));
    }

    @PostMapping
    @Operation(summary = "Send a message (legacy)")
    public ResponseEntity<MessageDto> sendLegacy(@RequestBody SendMessageRequest req) {
        Long senderId = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.sendLegacy(senderId, req));
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get conversation messages")
    public ResponseEntity<List<MessageDto>> getConversation(
            @PathVariable("id") String conversationId
    ) {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.getConversation(conversationId, me));
    }

    @GetMapping
    @Operation(summary = "Get conversation messages (legacy by job + peer)")
    public ResponseEntity<List<MessageDto>> getConversationLegacy(@RequestParam Long jobId,
                                                                  @RequestParam Long peerId) {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.getConversationByJobAndPeer(jobId, me, peerId));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List all my conversations")
    public ResponseEntity<List<ConversationSummaryDto>> getConversations() {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.getConversations(me));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread message count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(Map.of("unread", messageService.unreadCount(me)));
    }

    @PutMapping("/read")
    @Operation(summary = "Mark messages from a peer as read (legacy)")
    public ResponseEntity<Map<String, Integer>> markRead(@RequestParam Long jobId, @RequestParam Long peerId) {
        Long me = currentUserService.requireCurrentUser().getId();
        String conversationId = messageService.conversationId(jobId, me, peerId);
        List<MessageDto> messages = messageService.getConversation(conversationId, me);
        return ResponseEntity.ok(Map.of("markedRead", messages.size()));
    }

    @PostMapping("/schedule-meet")
    @Operation(summary = "Schedule Google Meet and send meet message")
    public ResponseEntity<ScheduleMeetResponse> scheduleMeet(@RequestBody ScheduleMeetRequest req) {
        Long me = currentUserService.requireCurrentUser().getId();
        return ResponseEntity.ok(messageService.scheduleMeet(me, req));
    }
}
