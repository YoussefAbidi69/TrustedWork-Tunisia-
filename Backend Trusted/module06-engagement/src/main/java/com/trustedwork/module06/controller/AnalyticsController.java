package com.trustedwork.module06.controller;

import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.AdvancedAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & ML", description = "Churn Risk (ML), Influence Score et Recommandations IA")
public class AnalyticsController {

    private final AdvancedAnalyticsService analyticsService;
    private final JwtUtil jwtUtil;

    /**
     * Tableau de bord analytique complet de l'utilisateur connecté.
     * Inclut le score d'influence, le risque de churn (formule) et les recommandations Groq.
     */
    @GetMapping("/me")
    @Operation(summary = "Analytics complets de l'utilisateur connecté")
    public ResponseEntity<Map<String, Object>> getMyAnalytics(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(Map.of(
            "influenceScore",   analyticsService.computeInfluenceScore(userId),
            "churnRisk",        analyticsService.predictChurnRisk(userId),
            "recommendations",  analyticsService.getAiRecommendations(userId)
        ));
    }

    /**
     * Prédiction ML complète via le modèle Random Forest Python.
     * Retourne : churn_probability, risk_label, recommendation, confidence.
     */
    @GetMapping("/churn-prediction/{userId}")
    @Operation(summary = "Prédiction de churn via modèle ML (Random Forest Python)")
    public ResponseEntity<Map<String, Object>> getChurnPrediction(
            @PathVariable Long userId) {
        return ResponseEntity.ok(analyticsService.getChurnPrediction(userId));
    }

    /**
     * Métriques académiques du modèle Random Forest entraîné.
     * Retourne : accuracy, precision, recall, F1, ROC-AUC, cross-validation, feature importance.
     */
    @GetMapping("/model/stats")
    @Operation(summary = "Métriques académiques du modèle IA (accuracy, F1, ROC-AUC...)")
    public ResponseEntity<Map<String, Object>> getModelStats() {
        return ResponseEntity.ok(analyticsService.getModelStats());
    }
}

