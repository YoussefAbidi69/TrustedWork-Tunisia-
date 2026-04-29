package com.trustedwork.module06.controller;

import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.AdvancedAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AdvancedAnalyticsService analyticsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        // Initialisation non requise pour le moment car MockitoExtension gère les mocks @Mock et @InjectMocks
    }

    // ✅ Test 1 : Succès complet
    @Test
    void testGetMyAnalytics_Success() {
        String mockToken = "Bearer dummy_token";
        when(jwtUtil.extractUserId("dummy_token")).thenReturn(5L);
        when(analyticsService.computeInfluenceScore(5L)).thenReturn(85.5);
        when(analyticsService.predictChurnRisk(5L)).thenReturn(10.0);

        Map<String, Object> aiRecs = Map.of(
                "events", List.of(Map.of("id", 1L, "title", "Hackathon")),
                "challenges", List.of()
        );
        when(analyticsService.getAiRecommendations(5L)).thenReturn(aiRecs);

        ResponseEntity<?> response = analyticsController.getMyAnalytics(mockToken);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode()); // ✅ Spring 6

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(85.5, body.get("influenceScore"));
        assertEquals(10.0, body.get("churnRisk"));
        assertEquals(aiRecs, body.get("recommendations"));
    }

    // ✅ Test 2 : Token invalide / utilisateur introuvable
    @Test
    void testGetMyAnalytics_InvalidToken() {
        String mockToken = "Bearer invalid_token";
        when(jwtUtil.extractUserId("invalid_token"))
                .thenThrow(new RuntimeException("Token invalide"));

        assertThrows(RuntimeException.class,
                () -> analyticsController.getMyAnalytics(mockToken));
    }

    // ✅ Test 3 : Recommandations vides
    @Test
    void testGetMyAnalytics_EmptyRecommendations() {
        String mockToken = "Bearer dummy_token";
        when(jwtUtil.extractUserId("dummy_token")).thenReturn(5L);
        when(analyticsService.computeInfluenceScore(5L)).thenReturn(0.0);
        when(analyticsService.predictChurnRisk(5L)).thenReturn(100.0);
        when(analyticsService.getAiRecommendations(5L))
                .thenReturn(Map.of("events", List.of(), "challenges", List.of()));

        ResponseEntity<?> response = analyticsController.getMyAnalytics(mockToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);

        Map<String, Object> recs = (Map<String, Object>) body.get("recommendations");
        assertTrue(((List<?>) recs.get("events")).isEmpty());
        assertTrue(((List<?>) recs.get("challenges")).isEmpty());
    }

    // ✅ Test 4 : Service analytics en erreur
    @Test
    void testGetMyAnalytics_ServiceDown() {
        String mockToken = "Bearer dummy_token";
        when(jwtUtil.extractUserId("dummy_token")).thenReturn(5L);
        when(analyticsService.computeInfluenceScore(5L))
                .thenThrow(new RuntimeException("Service indisponible"));

        assertThrows(RuntimeException.class,
                () -> analyticsController.getMyAnalytics(mockToken));
    }

    // ✅ Test 5 : Prédiction ML Churn
    @Test
    void testGetChurnPrediction_Success() {
        when(analyticsService.getChurnPrediction(5L)).thenReturn(Map.of(
                "churn_probability", 75.0,
                "risk_label", "HIGH"
        ));

        ResponseEntity<Map<String, Object>> response = analyticsController.getChurnPrediction(5L);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(75.0, response.getBody().get("churn_probability"));
        assertEquals("HIGH", response.getBody().get("risk_label"));
    }

    // ✅ Test 6 : Statistiques Modèle ML
    @Test
    void testGetModelStats_Success() {
        when(analyticsService.getModelStats()).thenReturn(Map.of(
                "algorithm", "Random Forest Classifier",
                "accuracy", 92.5
        ));

        ResponseEntity<Map<String, Object>> response = analyticsController.getModelStats();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(92.5, response.getBody().get("accuracy"));
        assertEquals("Random Forest Classifier", response.getBody().get("algorithm"));
    }
}