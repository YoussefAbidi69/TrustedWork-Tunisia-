package tn.esprit.freelancerprofileservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.services.MlServiceClient;
import tn.esprit.freelancerprofileservice.services.MlServiceClient.TrustScoreResult;

import java.util.Map;

/**
 * Controller REST — résultats ML exposés à Angular
 *
 * GET  /api/ml/profiles/user/{userId}/trust-score
 * POST /api/ml/reviews/analyze
 */
@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MlController {

    private final MlServiceClient mlServiceClient;
    private final FreelancerProfileRepository profileRepository;

    /**
     * Retourne le Trust Score ML d'un profil freelancer.
     * Appelé par Angular frontoffice pour afficher le badge de confiance.
     *
     * Les paramètres kycVerified et twoFactorEnabled viennent du user-service (Module 01).
     * Pour la démo jury : on les passe en query params.
     */
    @GetMapping("/profiles/user/{userId}/trust-score")
    public ResponseEntity<Map<String, Object>> getTrustScore(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean kycVerified,
            @RequestParam(defaultValue = "false") boolean twoFactorEnabled) {

        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FreelancerProfile", userId));

        TrustScoreResult result = mlServiceClient.predictTrustScore(
                profile, kycVerified, twoFactorEnabled
        );

        return ResponseEntity.ok(Map.of(
                "userId",     userId,
                "level",      result.level(),
                "confidence", result.confidence()
        ));
    }

    /**
     * Analyse le sentiment d'un commentaire — utilisé pour test/démo.
     * En production, l'analyse est automatique dans ProfileReviewServiceImpl.
     */
    @PostMapping("/reviews/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSentiment(
            @RequestBody Map<String, String> body) {

        String comment = body.get("comment");
        if (comment == null || comment.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        MlServiceClient.SentimentResult result = mlServiceClient.predictSentiment(comment);

        return ResponseEntity.ok(Map.of(
                "comment",   comment,
                "sentiment", result.sentiment(),
                "score",     result.score()
        ));
    }
}