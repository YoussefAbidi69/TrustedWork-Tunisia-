package tn.esprit.smartjobboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.smartjobboard.dto.*;
import tn.esprit.smartjobboard.service.CurrentUserService;
import tn.esprit.smartjobboard.service.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController")
class MessageControllerTest {

    @Mock private MessageService messageService;
    @Mock private CurrentUserService currentUserService;
    @InjectMocks private MessageController controller;

    private UserReferenceDto mockUser(Long id) {
        UserReferenceDto u = new UserReferenceDto();
        u.setId(id);
        u.setEmail("user@example.com");
        return u;
    }

    @Test
    @DisplayName("send should delegate to messageService.send and return 200")
    void send() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        MessageDto dto = MessageDto.builder().id(1L).content("Hello").build();
        when(messageService.send(eq("conv123"), eq(10L), any())).thenReturn(dto);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Hello");

        ResponseEntity<MessageDto> result = controller.send("conv123", req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("sendLegacy should delegate to messageService.sendLegacy")
    void sendLegacy() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        MessageDto dto = MessageDto.builder().id(1L).content("Hi").build();
        when(messageService.sendLegacy(eq(10L), any())).thenReturn(dto);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Hi");
        req.setJobOfferId(1L);
        req.setReceiverId(20L);

        ResponseEntity<MessageDto> result = controller.sendLegacy(req);

        assertThat(result.getBody().getContent()).isEqualTo("Hi");
    }

    @Test
    @DisplayName("getConversation should return messages")
    void getConversation() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        when(messageService.getConversation("conv123", 10L)).thenReturn(List.of());

        ResponseEntity<List<MessageDto>> result = controller.getConversation("conv123");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("getConversationLegacy should delegate to getConversationByJobAndPeer")
    void getConversationLegacy() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        when(messageService.getConversationByJobAndPeer(1L, 10L, 20L)).thenReturn(List.of());

        ResponseEntity<List<MessageDto>> result = controller.getConversationLegacy(1L, 20L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("getConversations should return summaries")
    void getConversations() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        when(messageService.getConversations(10L)).thenReturn(List.of());

        ResponseEntity<List<ConversationSummaryDto>> result = controller.getConversations();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("unreadCount should return map with unread count")
    void unreadCount() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        when(messageService.unreadCount(10L)).thenReturn(5L);

        ResponseEntity<Map<String, Long>> result = controller.unreadCount();

        assertThat(result.getBody()).containsEntry("unread", 5L);
    }

    @Test
    @DisplayName("markRead should delegate and return marked count")
    void markRead() {
        when(currentUserService.requireCurrentUser()).thenReturn(mockUser(10L));
        when(messageService.conversationId(1L, 10L, 20L)).thenReturn("conv_abc");
        when(messageService.getConversation("conv_abc", 10L)).thenReturn(
                List.of(MessageDto.builder().id(1L).build(), MessageDto.builder().id(2L).build()));

        ResponseEntity<Map<String, Integer>> result = controller.markRead(1L, 20L);

        assertThat(result.getBody()).containsEntry("markedRead", 2);
    }
}
