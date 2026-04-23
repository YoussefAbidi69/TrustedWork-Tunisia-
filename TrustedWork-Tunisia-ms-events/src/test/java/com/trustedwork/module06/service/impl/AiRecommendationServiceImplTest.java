package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.entity.Event;
import com.trustedwork.module06.repository.ChallengeRepository;
import com.trustedwork.module06.repository.EventRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private GrowthProfileRepository growthRepo;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AiRecommendationServiceImpl aiRecommendationService;

    @Test
    void testGetSmartRecommendations_BasicFlow() {
        Long userId = 1L;
        
        // Mock DB calls
        when(eventRepository.findAll()).thenReturn(List.of(Event.builder().id(10L).title("Event 1").build()));
        when(challengeRepository.findAll()).thenReturn(List.of(Challenge.builder().id(20L).title("Challenge 1").build()));
        
        // Mock Identity Service call
        Map<String, Object> userData = Map.of("firstName", "Amir", "bio", "Developer");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(userData);
        
        // Execute (it will probably fail the API call if API_KEY is missing, but we catch it)
        Map<String, Object> result = aiRecommendationService.getSmartRecommendations(userId);
        
        assertNotNull(result);
        assertTrue(result.containsKey("events"));
        assertTrue(result.containsKey("challenges"));
    }

    @Test
    void testGetSmartRecommendations_ApiError() {
        Long userId = 1L;
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("Groq Offline"));
        
        Map<String, Object> result = aiRecommendationService.getSmartRecommendations(userId);
        
        assertNotNull(result);
        assertEquals(0, ((List)result.get("events")).size());
    }
}
