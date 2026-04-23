package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.EventDTO;
import com.trustedwork.module06.entity.*;
import com.trustedwork.module06.enums.*;
import com.trustedwork.module06.exception.*;
import com.trustedwork.module06.mapper.EventMapper;
import com.trustedwork.module06.repository.*;
import com.trustedwork.module06.service.EventService;
import com.trustedwork.module06.service.GamificationService;
import com.trustedwork.module06.util.XPConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private static final String EVENT_NOT_FOUND_MSG = "Event not found";
    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final GamificationService gamificationService;

    @Override
    public EventDTO createEvent(EventDTO dto) {
        Event event = EventMapper.toEntity(dto);
        event.setRegisteredCount(0);
        determineEventStatus(event);
        return toDtoWithUsers(eventRepository.save(event));
    }

    @Override
    public List<EventDTO> getAllEvents() {
        return eventRepository.findAll()
                .stream().map(this::toDtoWithUsers).toList();
    }

    @Override
    public List<EventDTO> getEventsByGovernorate(String governorate) {
        return eventRepository.findByGovernorate(governorate)
                .stream().map(this::toDtoWithUsers).toList();
    }

    @Override
    public EventRegistration registerToEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG + ": " + eventId));

        if (registrationRepository.existsByEventIdAndUserId(eventId, userId))
            throw new AlreadyRegisteredException("User already registered to this event");

        if (event.getRegisteredCount() >= event.getCapacity())
            throw new IllegalStateException("Event is full");

        event.setRegisteredCount(event.getRegisteredCount() + 1);
        eventRepository.save(event);

        EventRegistration reg = EventRegistration.builder()
                .event(event).userId(userId)
                .status(RegistrationStatus.REGISTERED).build();

        EventRegistration saved = registrationRepository.save(reg);
        gamificationService.addXp(userId, XPConstants.EVENT_REGISTRATION_XP, "EVENT_REGISTRATION");
        return saved;
    }

    @Override
    public void markAttended(Long registrationId, Long userId) {
        EventRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        reg.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(reg);
        gamificationService.addXp(userId, XPConstants.EVENT_ATTENDED_XP, "EVENT_ATTENDED");
    }

    @Override
    public EventDTO updateEvent(Long id, EventDTO dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG));
        
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setType(dto.getType());
        event.setCity(dto.getCity());
        event.setGovernorate(dto.getGovernorate());
        event.setOnline(dto.isOnline());
        event.setCapacity(dto.getCapacity());
        event.setStartDate(dto.getStartDate());
        event.setEndDate(dto.getEndDate());
        
        determineEventStatus(event);
        
        return toDtoWithUsers(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG));
        eventRepository.delete(event);
    }

    private void determineEventStatus(Event event) {
        if (event.getStartDate() == null || event.getEndDate() == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getStartDate())) {
            event.setStatus(EventStatus.UPCOMING);
        } else if (now.isAfter(event.getEndDate())) {
            event.setStatus(EventStatus.COMPLETED);
        } else {
            event.setStatus(EventStatus.ONGOING);
        }
    }

    @Override
    public List<Long> getMyRegisteredEventIds(Long userId) {
        return registrationRepository.findByUserId(userId)
                .stream()
                .map(reg -> reg.getEvent().getId())
                .toList();
    }

    @Override
    public void cancelRegistration(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_NOT_FOUND_MSG));
        
        // Find registration
        List<EventRegistration> regs = registrationRepository.findByUserId(userId);
        regs.stream()
            .filter(r -> r.getEvent().getId().equals(eventId))
            .findFirst()
            .ifPresent(reg -> {
                // Delete registration
                registrationRepository.delete(reg);
                
                // Update event count
                event.setRegisteredCount(Math.max(0, event.getRegisteredCount() - 1));
                eventRepository.save(event);
                
                // Deduct XP
                gamificationService.addXp(userId, -XPConstants.EVENT_REGISTRATION_XP, "EVENT_CANCELLED");
            });
    }

    private EventDTO toDtoWithUsers(Event event) {
        List<Long> userIds = registrationRepository.findByEventId(event.getId())
                .stream()
                .map(EventRegistration::getUserId)
                .toList();
        return EventMapper.toDto(event, userIds);
    }
}
