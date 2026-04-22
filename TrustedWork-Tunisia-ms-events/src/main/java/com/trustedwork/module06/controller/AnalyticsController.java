package com.trustedwork.module06.controller;

import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.AdvancedAnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Churn, Influence et Matching")
public class AnalyticsController {

    private final AdvancedAnalyticsService analyticsService;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    public ResponseEntity<?> getMyAnalytics(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        return ResponseEntity.ok(Map.of(
            "influenceScore", analyticsService.computeInfluenceScore(userId),
            "churnRisk", analyticsService.predictChurnRisk(userId),
            "recommendations", analyticsService.getAiRecommendations(userId)
        ));
    }
}
