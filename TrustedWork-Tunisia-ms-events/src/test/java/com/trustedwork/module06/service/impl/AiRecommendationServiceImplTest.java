package com.trustedwork.module06.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.entity.Event;
import com.trustedwork.module06.repository.ChallengeRepository;
import com.trustedwork.module06.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceImplTest {

    @Mock
    private EventRepository eventRepo;

    @Mock
    private ChallengeRepository challengeRepo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiRecommendationServiceImpl aiService;

    private Event mockEvent;
    private Challenge mockChallenge;

    @BeforeEach
    void setUp() {
        // Inject fake configuration values
        ReflectionTestUtils.setField(aiService, "apiKey", "test-groq-key");
        ReflectionTestUtils.setField(aiService, "apiUrl", "https://api.groq.com/openai/v1/chat/completions");

        mockEvent = new Event();
        mockEvent.setId(1L);
        mockEvent.setTitle("Hackathon AI");
        mockEvent.setRegisteredCount(10);

        mockChallenge = new Challenge();
        mockChallenge.setId(1L);
        mockChallenge.setTitle("Defi Code Vert");
    }

    @Test
    void testGetSmartRecommendations_Success() throws Exception {
        // Arrange
        when(eventRepo.findAll()).thenReturn(List.of(mockEvent));
        when(challengeRepo.findAll()).thenReturn(List.of(mockChallenge));

        // Mock User Service response
        Map<String, Object> userProfile = Map.of("location", "Tunis");
        when(restTemplate.getForObject(contains("/users/5"), eq(Map.class)))
                .thenReturn(userProfile);

        // Mock Groq API response
        String aiResponseJson = "{" +
                "\"choices\": [{" +
                "\"message\": {" +
                "\"content\": \"{\\\"eventRecommendations\\\": [{\\\"id\\\": 1, \\\"reason\\\": \\\"Bon event\\\"}], \\\"challengeRecommendations\\\": [{\\\"id\\\": 1, \\\"reason\\\": \\\"Bon challenge\\\"}]}\"" +
                "}" +
                "}]" +
                "}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(aiResponseJson, HttpStatus.OK);
        
        when(restTemplate.postForEntity(eq("https://api.groq.com/openai/v1/chat/completions"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        Map<String, Object> result = aiService.getSmartRecommendations(5L);

        // Assert
        assertNotNull(result);
        List<?> events = (List<?>) result.get("events");
        List<?> challenges = (List<?>) result.get("challenges");
        
        assertEquals(1, events.size(), "Should recommend 1 event");
        assertEquals(1, challenges.size(), "Should recommend 1 challenge");
        
        Map<String, Object> firstEvent = (Map<String, Object>) events.get(0);
        assertEquals("Hackathon AI", firstEvent.get("title"));
    }

    @Test
    void testGetSmartRecommendations_FallbackMode() {
        // Arrange
        when(eventRepo.findAll()).thenReturn(List.of(mockEvent));
        when(challengeRepo.findAll()).thenReturn(List.of(mockChallenge));

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("User Service Down"));

        // Simulate Groq API failure
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Groq API Down"));

        // Act
        Map<String, Object> result = aiService.getSmartRecommendations(10L);

        // Assert
        assertNotNull(result);
        List<?> events = (List<?>) result.get("events");
        
        // Fallback mode logic is to return top registered events
        assertFalse(events.isEmpty(), "Fallback should return events");
        Map<String, Object> firstEvent = (Map<String, Object>) events.get(0);
        assertEquals("Incontournable en ce moment (Recommandé)", firstEvent.get("reason"));
    }

    @Test
    void testGetSmartRecommendations_EmptyData() {
        // Arrange
        when(eventRepo.findAll()).thenReturn(List.of());
        when(challengeRepo.findAll()).thenReturn(List.of());

        // Act
        Map<String, Object> result = aiService.getSmartRecommendations(1L);

        // Assert
        assertTrue(((List<?>) result.get("events")).isEmpty());
        assertTrue(((List<?>) result.get("challenges")).isEmpty());
    }
}
