package com.trustedwork.module06.controller;

import com.trustedwork.module06.dto.EventDTO;
import com.trustedwork.module06.entity.EventRegistration;
import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Gestion des événements")
public class EventController {

    private final EventService eventService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAll() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/governorate/{gov}")
    public ResponseEntity<List<EventDTO>> getByGovernorate(@PathVariable String gov) {
        return ResponseEntity.ok(eventService.getEventsByGovernorate(gov));
    }

    @PostMapping
    public ResponseEntity<EventDTO> create(@RequestBody EventDTO dto) {
        return ResponseEntity.ok(eventService.createEvent(dto));
    }

    @PostMapping("/{eventId}/register")
    public ResponseEntity<?> register(
            @PathVariable Long eventId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login to register"));
        }
        Long userId = jwtUtil.extractUserId(token.substring(7));
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid session, please login again"));
        }
        return ResponseEntity.ok(eventService.registerToEvent(eventId, userId));
    }

    @GetMapping("/my-registrations")
    public ResponseEntity<?> getMyRegisteredEvents(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "Please login"));
        }
        Long userId = jwtUtil.extractUserId(token.substring(7));
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid session"));
        return ResponseEntity.ok(eventService.getMyRegisteredEventIds(userId));
    }

    @PatchMapping("/registrations/{regId}/attend")
    public ResponseEntity<Void> markAttended(
            @PathVariable Long regId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        eventService.markAttended(regId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> update(@PathVariable Long id, @RequestBody EventDTO dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}/registrations/{userId}")
    public ResponseEntity<Void> cancelRegistration(@PathVariable Long eventId, @PathVariable Long userId) {
        eventService.cancelRegistration(eventId, userId);
        return ResponseEntity.ok().build();
    }
}
