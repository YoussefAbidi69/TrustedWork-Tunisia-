package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.EventDTO;
import com.trustedwork.module06.entity.Event;
import com.trustedwork.module06.entity.EventRegistration;
import com.trustedwork.module06.repository.EventRegistrationRepository;
import com.trustedwork.module06.repository.EventRepository;
import com.trustedwork.module06.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventRegistrationRepository registrationRepository;
    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void testRegisterToEvent_Success() {
        Long eventId = 1L;
        Long userId = 100L;
        Event event = Event.builder()
                .id(eventId)
                .capacity(10)
                .registeredCount(5)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
        when(registrationRepository.save(any())).thenReturn(new EventRegistration());

        EventRegistration result = eventService.registerToEvent(eventId, userId);

        assertNotNull(result);
        assertEquals(6, event.getRegisteredCount());
        verify(eventRepository).save(event);
        verify(gamificationService).addXp(eq(userId), anyInt(), anyString());
    }

    @Test
    void testCreateEvent() {
        EventDTO dto = EventDTO.builder()
                .title("Test Event")
                .capacity(50)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();

        when(eventRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        EventDTO result = eventService.createEvent(dto);

        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
        verify(eventRepository).save(any());
    }
}
