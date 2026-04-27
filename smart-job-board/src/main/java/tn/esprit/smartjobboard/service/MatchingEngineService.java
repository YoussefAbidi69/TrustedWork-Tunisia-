package tn.esprit.smartjobboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.entity.CompatibilityReport;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.MatchScore;
import tn.esprit.smartjobboard.entity.PredictionConfidence;
import tn.esprit.smartjobboard.entity.SuccessPrediction;
import tn.esprit.smartjobboard.repository.CompatibilityReportRepository;
import tn.esprit.smartjobboard.repository.MatchScoreRepository;
import tn.esprit.smartjobboard.repository.SuccessPredictionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Weighted match scoring, success prediction, and persisted breakdown rows.
 */
@Service
@RequiredArgsConstructor
public class MatchingEngineService {

    public static final double W_SKILL = 0.40;
    public static final double W_REPUTATION = 0.20;
    public static final double W_SUCCESS = 0.20;
    public static final double W_BUDGET = 0.10;
    public static final double W_AVAIL = 0.10;

    public static final double DEFAULT_REPUTATION = 70.0;
    public static final double DEFAULT_SUCCESS_RATE = 75.0;
    public static final double DEFAULT_AVAILABILITY = 80.0;

    private final SemanticSkillService semanticSkillService;
    private final MatchScoreRepository matchScoreRepository;
    private final SuccessPredictionRepository successPredictionRepository;
    private final CompatibilityReportRepository compatibilityReportRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public record MLMatchRequest(double skill_match, double reputation, double success_rate, double budget_fit, double availability) {}
    public record MLMatchResponse(double totalScore, double successProbability, String confidence) {}

    /**
     * Computes weighted scores without touching the database (used for ranking many jobs).
     */
    public RawMatchEvaluation evaluateRaw(JobOffer job, FreelancerProfile profile, BigDecimal rateForBudgetFit) {
        List<String> jobNeeds = mergeJobSkills(job);
        double skillPct = semanticSkillService.skillMatchPercent(jobNeeds, profile.getSkills());
        double reputation = DEFAULT_REPUTATION;
        double successRate = DEFAULT_SUCCESS_RATE;
        double availability = DEFAULT_AVAILABILITY;
        double budgetFit = computeBudgetFit(job, rateForBudgetFit);

        try {
            MLMatchRequest requestPayload = new MLMatchRequest(skillPct, reputation, successRate, budgetFit, availability);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MLMatchRequest> requestEntity = new HttpEntity<>(requestPayload, headers);
            
            ResponseEntity<MLMatchResponse> response = restTemplate.postForEntity("http://127.0.0.1:8000/predict", requestEntity, MLMatchResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                MLMatchResponse body = response.getBody();
                PredictionConfidence conf;
                try {
                    conf = PredictionConfidence.valueOf(body.confidence());
                } catch (Exception e) {
                    conf = confidenceFor(body.successProbability());
                }
                return new RawMatchEvaluation(
                        round2(skillPct), round2(reputation), round2(successRate), round2(budgetFit), round2(availability),
                        body.totalScore(), body.successProbability(), conf
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to call ML Matching Engine, falling back to heuristic: " + e.getMessage());
        }

        double total = skillPct * W_SKILL
                + reputation * W_REPUTATION
                + successRate * W_SUCCESS
                + budgetFit * W_BUDGET
                + availability * W_AVAIL;
        total = round2(Math.min(100.0, Math.max(0.0, total)));

        double skillOverlap = skillPct / 100.0;
        double probability = skillOverlap * 0.5 + (reputation / 100.0) * 0.3 + (successRate / 100.0) * 0.2;
        probability = round2(Math.min(1.0, Math.max(0.0, probability)));
        PredictionConfidence conf = confidenceFor(probability);

        return new RawMatchEvaluation(
                round2(skillPct), round2(reputation), round2(successRate), round2(budgetFit), round2(availability),
                total, probability, conf
        );
    }

    @Transactional
    public MatchComputationResult computePersistAndReturn(JobOffer job, FreelancerProfile profile, BigDecimal rateForBudgetFit) {
        RawMatchEvaluation raw = evaluateRaw(job, profile, rateForBudgetFit);
        return persistRaw(job, profile, raw);
    }

    @Transactional
    public MatchComputationResult persistRaw(JobOffer job, FreelancerProfile profile, RawMatchEvaluation raw) {
        MatchScore ms = matchScoreRepository.findByJobOfferIdAndFreelancerId(job.getId(), profile.getUserId())
                .orElseGet(MatchScore::new);
        ms.setJobOfferId(job.getId());
        ms.setFreelancerId(profile.getUserId());
        ms.setSkillMatch(raw.skillMatch());
        ms.setReputation(raw.reputation());
        ms.setSuccessRate(raw.successRate());
        ms.setBudgetFit(raw.budgetFit());
        ms.setAvailability(raw.availability());
        ms.setTotalScore(raw.totalScore());
        ms = matchScoreRepository.save(ms);

        SuccessPrediction sp = successPredictionRepository.findByJobOfferIdAndFreelancerId(job.getId(), profile.getUserId())
                .orElseGet(SuccessPrediction::new);
        sp.setJobOfferId(job.getId());
        sp.setFreelancerId(profile.getUserId());
        sp.setProbability(raw.successProbability());
        sp.setConfidence(raw.confidence());
        successPredictionRepository.save(sp);

        String summaryJson = buildSummaryJson(ms, raw.successProbability(), raw.confidence().name());
        CompatibilityReport cr = compatibilityReportRepository.findByMatchScoreId(ms.getId())
                .orElseGet(CompatibilityReport::new);
        cr.setMatchScoreId(ms.getId());
        cr.setJobOfferId(job.getId());
        cr.setFreelancerId(profile.getUserId());
        cr.setSummaryJson(summaryJson);
        compatibilityReportRepository.save(cr);

        return new MatchComputationResult(ms, sp, cr);
    }

    private PredictionConfidence confidenceFor(double p) {
        if (p < 0.4) {
            return PredictionConfidence.LOW;
        }
        if (p <= 0.7) {
            return PredictionConfidence.MEDIUM;
        }
        return PredictionConfidence.HIGH;
    }

    public record RawMatchEvaluation(double skillMatch, double reputation, double successRate, double budgetFit,
                                     double availability, double totalScore, double successProbability,
                                     PredictionConfidence confidence) {
    }

    private String buildSummaryJson(MatchScore ms, double probability, String confidence) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "skillMatch", ms.getSkillMatch(),
                    "reputation", ms.getReputation(),
                    "successRate", ms.getSuccessRate(),
                    "budgetFit", ms.getBudgetFit(),
                    "availability", ms.getAvailability(),
                    "totalScore", ms.getTotalScore(),
                    "successProbability", probability,
                    "confidence", confidence
            ));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"json\"}";
        }
    }

    public List<String> mergeJobSkills(JobOffer job) {
        Set<String> set = new LinkedHashSet<>();
        if (job.getRequiredSkills() != null) {
            job.getRequiredSkills().stream().filter(s -> s != null && !s.isBlank()).forEach(set::add);
        }
        if (job.getExtractedSkills() != null) {
            job.getExtractedSkills().stream().filter(s -> s != null && !s.isBlank()).forEach(set::add);
        }
        return new ArrayList<>(set);
    }

    public double computeBudgetFit(JobOffer job, BigDecimal proposedRate) {
        BigDecimal min = job.getBudgetMin();
        BigDecimal max = job.getBudgetMax();
        if (min == null || max == null || proposedRate == null) {
            return 0.0;
        }
        if (proposedRate.compareTo(min) >= 0 && proposedRate.compareTo(max) <= 0) {
            return 100.0;
        }
        if (proposedRate.compareTo(min) < 0) {
            if (min.signum() == 0) {
                return 0.0;
            }
            BigDecimal gap = min.subtract(proposedRate);
            double pct = gap.divide(min, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            return round2(Math.max(0.0, 100.0 - pct));
        }
        BigDecimal gap = proposedRate.subtract(max);
        if (max.signum() == 0) {
            return 0.0;
        }
        double pct = gap.divide(max, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        return round2(Math.max(0.0, 100.0 - pct));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public record MatchComputationResult(MatchScore matchScore, SuccessPrediction successPrediction,
                                         CompatibilityReport compatibilityReport) {
    }
}
