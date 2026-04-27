package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based fraud scoring for job offers with explainable triggered signals.
 */
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private static final BigDecimal BUDGET_SUSPICIOUS_MAX = BigDecimal.valueOf(50);

    private final JobOfferRepository jobOfferRepository;

    public FraudAssessment assess(JobOffer job, Long excludeJobIdForDuplicateTitle) {
        List<TriggeredSignal> triggers = new ArrayList<>();
        double score = 0.0;

        if (job.getBudgetMax() != null && job.getBudgetMax().compareTo(BUDGET_SUSPICIOUS_MAX) < 0) {
            add(triggers, "LOW_BUDGET", "Maximum budget is below 50 — unusually low for a full project.", 0.30);
            score += 0.30;
        }
        if (job.getDescription() == null || job.getDescription().length() < 100) {
            add(triggers, "SHORT_DESCRIPTION", "Description shorter than 100 characters.", 0.20);
            score += 0.20;
        }
        if (job.getTitle() == null || job.getTitle().length() < 10) {
            add(triggers, "SHORT_TITLE", "Title shorter than 10 characters.", 0.15);
            score += 0.15;
        }
        long recentJobs = jobOfferRepository.countByClientIdAndCreatedAtAfter(
                job.getClientId(), LocalDateTime.now().minusHours(24));
        if (recentJobs > 5) {
            add(triggers, "HIGH_POSTING_VELOCITY", "Client posted more than 5 jobs in the last 24 hours.", 0.25);
            score += 0.25;
        }
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long dupTitle = jobOfferRepository.countDuplicateTitleSince(
                job.getClientId(), job.getTitle(), weekAgo, excludeJobIdForDuplicateTitle);
        if (dupTitle > 0) {
            add(triggers, "DUPLICATE_TITLE", "Identical job title reused within the last 7 days.", 0.35);
            score += 0.35;
        }
        if (job.getRequiredSkills() == null || job.getRequiredSkills().stream().allMatch(s -> s == null || s.isBlank())) {
            add(triggers, "NO_REQUIRED_SKILLS", "No required skills were listed.", 0.15);
            score += 0.15;
        }

        score = Math.min(1.0, score);
        return new FraudAssessment(score, triggers);
    }

    private void add(List<TriggeredSignal> triggers, String code, String message, double weight) {
        triggers.add(new TriggeredSignal(code, message, weight));
    }

    public record FraudAssessment(double fraudRiskScore, List<TriggeredSignal> triggers) {
    }

    public record TriggeredSignal(String code, String message, double weight) {
    }
}
