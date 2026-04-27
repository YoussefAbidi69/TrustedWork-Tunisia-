package tn.esprit.freelancerprofileservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.response.ProfileViewAnalyticsResponse;
import tn.esprit.freelancerprofileservice.services.IProfileViewService;

import java.util.Map;

/**
 * Controller REST — analytics de visites de profils.
 *
 * Endpoints :
 * - POST /api/views/profiles/{profileId}?viewerId=X  → enregistre une vue
 * - GET  /api/views/profiles/{profileId}/count       → nombre total de vues
 * - GET  /api/views/profiles/{profileId}/analytics   → analytics complètes
 *
 * Tous les endpoints sont en permitAll() dans SecurityConfig — pas de JWT requis.
 * Anti-spam et anti-self-view gérés dans ProfileViewServiceImpl.
 */
@RestController
@RequestMapping("/api/views")
@RequiredArgsConstructor
public class ProfileViewController {

    private final IProfileViewService profileViewService;

    /**
     * POST /api/views/profiles/{profileId}?viewerId=X
     * Enregistre une vue sur le profil.
     * viewerId optionnel — null si visiteur anonyme.
     */
    @PostMapping("/profiles/{profileId}")
    public ResponseEntity<Map<String, String>> recordView(
            @PathVariable Long profileId,
            @RequestParam(required = false) Long viewerId) {

        profileViewService.recordView(profileId, viewerId);
        return ResponseEntity.ok(Map.of("message", "Vue enregistrée"));
    }

    /**
     * GET /api/views/profiles/{profileId}/count
     * Retourne le nombre total de vues.
     */
    @GetMapping("/profiles/{profileId}/count")
    public ResponseEntity<Long> getTotalViews(@PathVariable Long profileId) {
        return ResponseEntity.ok(profileViewService.getTotalViews(profileId));
    }

    /**
     * GET /api/views/profiles/{profileId}/analytics
     * Retourne les analytics : total vues, visiteurs uniques, vues 7 derniers jours.
     */
    @GetMapping("/profiles/{profileId}/analytics")
    public ResponseEntity<ProfileViewAnalyticsResponse> getAnalytics(
            @PathVariable Long profileId) {
        return ResponseEntity.ok(profileViewService.getAnalytics(profileId));
    }
}