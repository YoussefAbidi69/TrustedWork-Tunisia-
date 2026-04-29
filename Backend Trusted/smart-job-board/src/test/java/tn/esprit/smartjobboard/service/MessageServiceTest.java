package tn.esprit.smartjobboard.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.dto.*;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.Message;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService")
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private FreelancerProfileClient freelancerProfileClient;
    @Mock private CurrentUserService currentUserService;
    @Mock private GoogleMeetService googleMeetService;

    @InjectMocks
    private MessageService service;

    @Nested
    @DisplayName("conversationId()")
    class ConversationIdGeneration {
        @Test
        @DisplayName("should generate stable conversation id regardless of user order")
        void stableOrder() {
            String id1 = service.conversationId(1L, 10L, 20L);
            String id2 = service.conversationId(1L, 20L, 10L);
            assertThat(id1).isEqualTo(id2);
            assertThat(id1).startsWith("c_");
        }
    }

    @Nested
    @DisplayName("send()")
    class Send {
        @Test
        @DisplayName("should throw if content is blank")
        void blankContent() {
            SendMessageRequest req = new SendMessageRequest();
            req.setContent("   ");

            assertThatThrownBy(() -> service.send("c_123", 5L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("should throw if conversation is new but missing receiverId/jobOfferId")
        void newMissingArgs() {
            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Hello");

            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.send("c_123", 5L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("should send new message in new conversation")
        void sendNew() {
            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Hello");
            req.setReceiverId(20L);
            req.setJobOfferId(1L);

            String cid = service.conversationId(1L, 5L, 20L);

            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(cid))
                    .thenReturn(Optional.empty());
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(new JobOffer()));

            Message saved = Message.builder().id(100L).content("Hello").senderId(5L).build();
            when(messageRepository.save(any())).thenReturn(saved);

            MessageDto result = service.send(cid, 5L, req);

            assertThat(result.getContent()).isEqualTo("Hello");
            verify(messageRepository).save(argThat(m ->
                    m.getConversationId().equals(cid) &&
                    m.getSenderId().equals(5L) &&
                    m.getReceiverId().equals(20L) &&
                    m.getJobOfferId().equals(1L)
            ));
        }

        @Test
        @DisplayName("should send message in existing conversation")
        void sendExisting() {
            SendMessageRequest req = new SendMessageRequest();
            req.setContent("Reply");

            Message latest = Message.builder()
                    .conversationId("c_abc")
                    .senderId(20L)
                    .receiverId(5L)
                    .jobOfferId(1L)
                    .build();

            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc("c_abc"))
                    .thenReturn(Optional.of(latest));

            Message saved = Message.builder().id(101L).content("Reply").senderId(5L).build();
            when(messageRepository.save(any())).thenReturn(saved);

            MessageDto result = service.send("c_abc", 5L, req);

            assertThat(result.getContent()).isEqualTo("Reply");
            // sender is 5L, receiver is 20L
            verify(messageRepository).save(argThat(m ->
                    m.getSenderId().equals(5L) && m.getReceiverId().equals(20L)
            ));
        }
    }

    @Nested
    @DisplayName("getConversation()")
    class GetConversation {

        @Test
        @DisplayName("should throw if conversation not found")
        void notFound() {
            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getConversation("c_123", 5L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should mark read and return messages")
        void returnMessages() {
            Message latest = Message.builder()
                    .senderId(20L)
                    .receiverId(5L)
                    .build();

            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc("c_123"))
                    .thenReturn(Optional.of(latest));

            Message m1 = Message.builder().id(1L).content("A").senderId(20L).build();
            Message m2 = Message.builder().id(2L).content("B").senderId(5L).build();
            when(messageRepository.findConversation("c_123")).thenReturn(List.of(m1, m2));

            List<MessageDto> result = service.getConversation("c_123", 5L);

            assertThat(result).hasSize(2);
            verify(messageRepository).markConversationRead("c_123", 20L, 5L);
        }
    }

    @Nested
    @DisplayName("getConversations()")
    class GetConversations {

        @Test
        @DisplayName("should group messages and return summaries")
        void getSummaries() {
            Message m1 = Message.builder()
                    .id(1L).conversationId("c_1").senderId(20L).receiverId(5L).jobOfferId(1L)
                    .content("Hello")
                    .createdAt(LocalDateTime.now())
                    .isRead(false).build();
            Message m2 = Message.builder()
                    .id(2L).conversationId("c_1").senderId(5L).receiverId(20L).jobOfferId(1L)
                    .content("Hi")
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .isRead(true).build();

            when(messageRepository.findAllForUser(5L)).thenReturn(List.of(m1, m2));

            JobOffer job = new JobOffer();
            job.setTitle("Test Job");
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            UserReferenceDto peer = new UserReferenceDto();
            peer.setFirstName("John");
            peer.setLastName("Doe");
            when(freelancerProfileClient.fetchFreelancerProfile(20L)).thenReturn(peer);

            List<ConversationSummaryDto> result = service.getConversations(5L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUnreadCount()).isEqualTo(1L);
            assertThat(result.get(0).getPeerName()).isEqualTo("John Doe");
            assertThat(result.get(0).getJobTitle()).isEqualTo("Test Job");
            assertThat(result.get(0).getLastMessage()).isEqualTo("Hello");
        }
    }

    @Nested
    @DisplayName("scheduleMeet()")
    class ScheduleMeet {
        @Test
        @DisplayName("should throw if unauthorized")
        void unauthorized() {
            ScheduleMeetRequest req = new ScheduleMeetRequest();
            req.setConversationId("c_123");

            Message latest = Message.builder().senderId(10L).receiverId(20L).build();
            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc("c_123"))
                    .thenReturn(Optional.of(latest));

            assertThatThrownBy(() -> service.scheduleMeet(99L, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should create meet and save message")
        void createMeet() {
            ScheduleMeetRequest req = new ScheduleMeetRequest();
            req.setConversationId("c_123");
            req.setDate("2024-05-01");
            req.setTime("10:00:00");
            req.setTitle("Interview");
            req.setDuration(30);

            Message latest = Message.builder().senderId(5L).receiverId(20L).jobOfferId(1L).build();
            when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc("c_123"))
                    .thenReturn(Optional.of(latest));

            UserReferenceDto me = new UserReferenceDto();
            me.setEmail("me@test.com");
            when(currentUserService.requireCurrentUser()).thenReturn(me);

            UserReferenceDto peer = new UserReferenceDto();
            peer.setEmail("peer@test.com");
            when(freelancerProfileClient.fetchFreelancerProfile(20L)).thenReturn(peer);

            GoogleMeetService.ScheduleResult meetRes = new GoogleMeetService.ScheduleResult("http://meet.google.com/abc", "event1");
            when(googleMeetService.createMeet(any(), any(), any(), anyInt(), anyString(), anyString()))
                    .thenReturn(meetRes);

            ScheduleMeetResponse result = service.scheduleMeet(5L, req);

            assertThat(result.getMeetUrl()).isEqualTo("http://meet.google.com/abc");
            verify(messageRepository).save(argThat(m ->
                    m.getType().equals("meet") && m.getMeetUrl().equals("http://meet.google.com/abc")
            ));
        }
    }
}

