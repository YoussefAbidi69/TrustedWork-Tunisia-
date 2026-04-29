package com.trustedwork.module06.controller;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.dto.GrowthProfileDTO;
import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.EngagementScoreService;
import com.trustedwork.module06.service.GamificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "XP, badges et streaks")
public class GamificationController {

    private final GamificationService gamificationService;
    private final EngagementScoreService scoreService;
    private final JwtUtil jwtUtil;

    @GetMapping("/profile")
    public ResponseEntity<?> getMyProfile(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired session"));
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }

    @GetMapping("/badges")
    public ResponseEntity<?> getMyBadges(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired session"));
        return ResponseEntity.ok(gamificationService.getUserBadges(userId));
    }

    @GetMapping("/score")
    public ResponseEntity<?> getScore(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "Invalid or expired session"));
        double score = scoreService.computeEngagementScore(userId);
        return ResponseEntity.ok(Map.of("engagementScore", score));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/profiles")
    public ResponseEntity<List<GrowthProfileDTO>> getAllProfiles() {
        return ResponseEntity.ok(gamificationService.getAllProfiles());
    }

    @GetMapping("/admin/user/{userId}/profile")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        // Admin can view any profile
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }

    @GetMapping("/admin/user/{userId}/badges")
    public ResponseEntity<?> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(gamificationService.getUserBadges(userId));
    }

    @GetMapping("/admin/user/{userId}/score")
    public ResponseEntity<?> getUserScore(@PathVariable Long userId) {
        double score = scoreService.computeEngagementScore(userId);
        return ResponseEntity.ok(Map.of("engagementScore", score));
    }

    @DeleteMapping("/admin/user/{userId}/badges/{badgeId}")
    public ResponseEntity<Void> removeUserBadge(@PathVariable Long userId, @PathVariable Long badgeId) {
        gamificationService.removeBadge(userId, badgeId);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(String token) {
        if (token == null || !token.startsWith("Bearer ")) return null;
        return jwtUtil.extractUserId(token.substring(7));
    }
}
