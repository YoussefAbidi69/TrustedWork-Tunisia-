package com.trustedwork.module06.service;

import com.trustedwork.module06.dto.EventDTO;
import com.trustedwork.module06.entity.EventRegistration;
import java.util.List;

public interface EventService {
    EventDTO createEvent(EventDTO dto);
    List<EventDTO> getAllEvents();
    List<EventDTO> getEventsByGovernorate(String governorate);
    EventRegistration registerToEvent(Long eventId, Long userId);
    void markAttended(Long registrationId, Long userId);
    EventDTO updateEvent(Long id, EventDTO dto);
    void deleteEvent(Long id);
    List<Long> getMyRegisteredEventIds(Long userId);
    void cancelRegistration(Long eventId, Long userId);
}
