package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import tn.esprit.freelancerprofileservice.config.MlServiceConfig;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.WorkExperienceRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReviewRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Client REST vers le micro-service ML Python (Flask port 5000).
 * Appelle deux endpoints :
 *   - POST /predict/sentiment    → analyse d'un commentaire d'avis
 *   - POST /predict/trust-score  → niveau de confiance d'un profil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MlServiceClient {

    private final RestTemplate restTemplate;
    private final SkillRepository skillRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final CertificationRepository certificationRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final ProfileReviewRepository profileReviewRepository;

    // -------------------------------------------------------
    // Analyse de sentiment d'un commentaire
    // -------------------------------------------------------

    /**
     * Envoie un commentaire au service ML et retourne le sentiment.
     * En cas d'erreur (service ML indisponible), retourne UNKNOWN.
     */
    public SentimentResult predictSentiment(String comment) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("comment", comment);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    MlServiceConfig.ML_SERVICE_URL + "/predict/sentiment",
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> result = response.getBody();
            String sentiment = (String) result.get("sentiment");
            Double score = ((Number) result.get("score")).doubleValue();

            log.info("[ML] Sentiment prédit : {} (score={})", sentiment, score);
            return new SentimentResult(sentiment, score);

        } catch (Exception e) {
            // Service ML indisponible — on ne bloque pas le flow métier
            log.warn("[ML] Service ML indisponible pour sentiment : {}", e.getMessage());
            return new SentimentResult("UNKNOWN", 0.0);
        }
    }

    // -------------------------------------------------------
    // Prédiction du Trust Score d'un profil
    // -------------------------------------------------------

    /**
     * Calcule le Trust Score ML d'un profil freelancer.
     * Collecte les features depuis la base de données.
     */
    public TrustScoreResult predictTrustScore(FreelancerProfile profile, boolean kycVerified, boolean twoFactorEnabled) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construction des features à partir des données du profil
            long skillCount        = skillRepository.countByProfileId(profile.getId());
            long portfolioCount    = portfolioItemRepository.countByProfileId(profile.getId());
            long certifCount       = certificationRepository.countByProfileId(profile.getId());
            long workExpMonths     = workExperienceRepository.sumMonthsByProfileId(profile.getId());
            long reviewCount       = profileReviewRepository.countByProfileId(profile.getId());
            double avgRating       = profileReviewRepository.avgRatingByProfileId(profile.getId());

            Map<String, Object> body = new HashMap<>();
            body.put("completeness_score",      profile.getCompletenessScore());
            body.put("skill_count",             skillCount);
            body.put("endorsement_count",       skillRepository.sumEndorsementsByProfileId(profile.getId()));
            body.put("portfolio_count",         portfolioCount);
            body.put("certification_count",     certifCount);
            body.put("work_experience_months",  workExpMonths);
            body.put("review_count",            reviewCount);
            body.put("avg_rating",              avgRating);
            body.put("kyc_verified",            kycVerified ? 1 : 0);
            body.put("two_factor_enabled",      twoFactorEnabled ? 1 : 0);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    MlServiceConfig.ML_SERVICE_URL + "/predict/trust-score",
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            Map<String, Object> result = response.getBody();
            String level      = (String) result.get("level");
            Double confidence = ((Number) result.get("confidence")).doubleValue();

            log.info("[ML] Trust Score prédit : {} (confidence={})", level, confidence);
            return new TrustScoreResult(level, confidence);

        } catch (Exception e) {
            log.warn("[ML] Service ML indisponible pour trust score : {}", e.getMessage());
            return new TrustScoreResult("UNKNOWN", 0.0);
        }
    }

    // -------------------------------------------------------
    // Classes résultat internes
    // -------------------------------------------------------

    public record SentimentResult(String sentiment, Double score) {}
    public record TrustScoreResult(String level, Double confidence) {}
}