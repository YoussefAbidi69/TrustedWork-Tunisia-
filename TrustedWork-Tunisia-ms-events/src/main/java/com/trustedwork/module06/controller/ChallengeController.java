package com.trustedwork.module06.controller;

import com.trustedwork.module06.dto.ChallengeDTO;
import com.trustedwork.module06.security.JwtUtil;
import com.trustedwork.module06.service.ChallengeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@CrossOrigin("*")
@Tag(name = "Challenges", description = "Community missions and rewards")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final JwtUtil jwtUtil;
    private static final String MESSAGE_KEY = "message";

    // ==================== USER ENDPOINTS ====================

    @GetMapping
    public ResponseEntity<List<ChallengeDTO>> getActiveChallenges(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        // If no user, return general list (or null status)
        return ResponseEntity.ok(challengeService.getActiveChallenges(userId != null ? userId : -1L));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, String>> joinChallenge(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            challengeService.joinChallenge(userId, id);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Mission joined! Time to complete your tasks."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    @PostMapping("/{id}/succeed")
    public ResponseEntity<Map<String, String>> succeedChallenge(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            challengeService.succeedChallenge(userId, id);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Mission successful! You can now claim your reward."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<Map<String, String>> claimReward(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = getUserId(token);
        if (userId == null) return ResponseEntity.status(401).build();
        try {
            challengeService.claimReward(userId, id);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Reward claimed! XP points added to your profile."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, e.getMessage()));
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin")
    public ResponseEntity<List<ChallengeDTO>> getAllChallenges() {
        return ResponseEntity.ok(challengeService.getAllChallenges());
    }

    @PostMapping("/admin")
    public ResponseEntity<ChallengeDTO> createChallenge(@RequestBody ChallengeDTO challengeDTO) {
        return ResponseEntity.ok(challengeService.createChallenge(challengeDTO));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<ChallengeDTO> updateChallenge(
            @PathVariable Long id, 
            @RequestBody ChallengeDTO challengeDTO) {
        return ResponseEntity.ok(challengeService.updateChallenge(id, challengeDTO));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(String token) {
        if (token == null || !token.startsWith("Bearer ")) return null;
        return jwtUtil.extractUserId(token.substring(7));
    }
}
