package com.trustedwork.module06.service;

import com.trustedwork.module06.entity.GrowthProfile;
import com.trustedwork.module06.entity.Streak;
import com.trustedwork.module06.service.impl.MlPredictionServiceImpl;
import com.trustedwork.module06.repository.ChallengeParticipationRepository;
import com.trustedwork.module06.repository.EventRegistrationRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.repository.StreakRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlPredictionServiceImplTest {

    @Mock
    private GrowthProfileRepository growthRepo;
    
    @Mock
    private StreakRepository streakRepo;
    
    @Mock
    private UserBadgeRepository userBadgeRepo;
    
    @Mock
    private EventRegistrationRepository eventRegistrationRepo;
    
    @Mock
    private ChallengeParticipationRepository challengeParticipationRepo;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MlPredictionServiceImpl mlPredictionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mlPredictionService, "aiServiceUrl", "http://localhost:5001");
        // Mocks par défaut pour éviter NPE
        lenient().when(userBadgeRepo.findByUserId(anyLong())).thenReturn(java.util.Collections.emptyList());
        lenient().when(challengeParticipationRepo.findByUserId(anyLong())).thenReturn(java.util.Collections.emptyList());
    }

    @Test
    void predictChurnRisk_Success() {
        GrowthProfile profile = new GrowthProfile();
        profile.setUserId(1L);
        profile.setLevel(5);

        when(growthRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(streakRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of());
        when(eventRegistrationRepo.countByUserId(1L)).thenReturn(0L);
        when(challengeParticipationRepo.findByUserId(1L)).thenReturn(List.of());

        Map<String, Object> mockResponse = Map.of(
                "churn_probability", 20.0,
                "risk_label", "LOW",
                "recommendation", "All good"
        );
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        Map<String, Object> result = mlPredictionService.predictChurnRisk(1L);

        assertNotNull(result);
        assertEquals(20.0, result.get("churn_probability"));
        assertEquals("LOW", result.get("risk_label"));
    }

    @Test
    void predictChurnRisk_Fallback() {
        GrowthProfile profile = new GrowthProfile();
        profile.setUserId(1L);
        profile.setEngagementScore(85.0);

        when(growthRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(streakRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of());
        when(eventRegistrationRepo.countByUserId(1L)).thenReturn(0L);
        when(challengeParticipationRepo.findByUserId(1L)).thenReturn(List.of());

        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection Refused"));

        Map<String, Object> result = mlPredictionService.predictChurnRisk(1L);

        assertNotNull(result);
        assertEquals("HIGH", result.get("risk_label"));
        assertTrue(result.containsKey("recommendation"));
    }

    @Test
    void getModelStats_Success() {
        Map<String, Object> mockResponse = Map.of("accuracy", 95.0);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        Map<String, Object> result = mlPredictionService.getModelStats();

        assertNotNull(result);
        assertEquals(95.0, result.get("accuracy"));
    }

    @Test
    void getModelStats_Fallback() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Connection Refused"));

        Map<String, Object> result = mlPredictionService.getModelStats();

        assertNotNull(result);
        assertEquals("Service ML indisponible", result.get("error"));
    }

    @Test
    void isServiceAvailable_Success() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
        assertTrue(mlPredictionService.isServiceAvailable());
    }

    @Test
    void isServiceAvailable_Failure() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Down"));
        assertFalse(mlPredictionService.isServiceAvailable());
    }

    @Test
    void predictChurnRisk_Fallback_MediumRisk() {
        GrowthProfile profile = new GrowthProfile();
        profile.setUserId(1L);
        profile.setEngagementScore(50.0);

        when(growthRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(streakRepo.findByUserId(1L)).thenReturn(Optional.of(Streak.builder().lastActivityDate(LocalDate.now().minusDays(8)).currentStreak(0).build()));
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of());
        when(eventRegistrationRepo.countByUserId(1L)).thenReturn(0L);
        when(challengeParticipationRepo.findByUserId(1L)).thenReturn(List.of());

        // mock AI down
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Down"));

        Map<String, Object> result = mlPredictionService.predictChurnRisk(1L);
        // Risk Score: 0.25 (inactivity) + 0 (streak>0) + 0 (score>30) = 0.25 -> 25% -> LOW (wait, 25 is < 40)
        // Let's adjust to get MEDIUM (>=40)
        // riskScore = 0.4 (>=14j) + 0.3 (streak=0) = 0.7 -> HIGH
        // riskScore = 0.25 (>=7j) + 0.3 (streak=0) = 0.55 -> MEDIUM? No 55 is >= 50 predicted but label?
        // Label: HIGH (>=70), MEDIUM (>=40), LOW (<40)
        // 0.25 + 0.3 = 0.55 -> 55% -> MEDIUM
        
        assertNotNull(result);
        assertEquals("MEDIUM", result.get("risk_label"));
    }

    @Test
    void predictChurnRisk_Fallback_LowRisk() {
        GrowthProfile profile = new GrowthProfile();
        profile.setUserId(1L);
        profile.setEngagementScore(90.0);

        when(growthRepo.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(streakRepo.findByUserId(1L)).thenReturn(Optional.of(Streak.builder().lastActivityDate(LocalDate.now().minusDays(1)).currentStreak(5).build()));
        when(userBadgeRepo.findByUserId(1L)).thenReturn(List.of());
        when(eventRegistrationRepo.countByUserId(1L)).thenReturn(0L);
        when(challengeParticipationRepo.findByUserId(1L)).thenReturn(List.of());

        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.POST), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Down"));

        Map<String, Object> result = mlPredictionService.predictChurnRisk(1L);
        // riskScore = 0 (inactive < 3j) + 0 (streak > 0) + 0 (score > 30) = 0 -> LOW
        assertEquals("LOW", result.get("risk_label"));
    }

    @Test
    void predictChurnRisk_ApiNullBody_FallsBack() {
        when(growthRepo.findByUserId(anyLong())).thenReturn(Optional.of(new GrowthProfile()));
        when(streakRepo.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(userBadgeRepo.findByUserId(anyLong())).thenReturn(List.of());
        when(eventRegistrationRepo.countByUserId(anyLong())).thenReturn(0L);
        when(challengeParticipationRepo.findByUserId(anyLong())).thenReturn(List.of());

        when(restTemplate.exchange(anyString(), any(), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        Map<String, Object> result = mlPredictionService.predictChurnRisk(1L);
        assertNotNull(result);
        assertEquals("Fallback (Python service down)", result.get("model"));
    }

    @Test
    void predictChurnRisk_ApiError_ReturnsDefault() {
        when(restTemplate.exchange(anyString(), any(), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        Map<String, Object> result = mlPredictionService.getModelStats();
        assertEquals("Service ML indisponible", result.get("error"));
    }

    @Test
    void buildFallbackResponse_InvalidFeatureTypes() {
        // Simuler des features avec des types invalides (pas des Number)
        Map<String, Object> invalidFeatures = new HashMap<>();
        invalidFeatures.put("days_inactive", "invalid");
        invalidFeatures.put("current_streak", null);
        invalidFeatures.put("engagement_score", java.util.List.of());

        // On utilise ReflectionTestUtils pour appeler la méthode privée
        Map<String, Object> result = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                mlPredictionService, "buildFallbackResponse", 1L, invalidFeatures);

        assertNotNull(result);
        assertEquals(90.0, result.get("churn_probability")); 
        assertEquals("HIGH", result.get("risk_label"));
    }

    @Test
    void buildFeatureVector_StreakWithoutDate() {
        when(growthRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(streakRepo.findByUserId(1L)).thenReturn(Optional.of(Streak.builder().lastActivityDate(null).build()));
        when(userBadgeRepo.findByUserId(1L)).thenReturn(java.util.Collections.emptyList());
        when(eventRegistrationRepo.countByUserId(1L)).thenReturn(0L);
        when(challengeParticipationRepo.findByUserId(1L)).thenReturn(java.util.Collections.emptyList());

        Map<String, Object> features = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                mlPredictionService, "buildFeatureVector", 1L);

        assertEquals(30L, features.get("days_inactive"));
    }
}
