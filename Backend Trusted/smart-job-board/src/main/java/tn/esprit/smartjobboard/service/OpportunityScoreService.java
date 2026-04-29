package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.entity.BudgetIntelligence;
import tn.esprit.smartjobboard.entity.JobDemandSnapshot;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.BudgetIntelligenceRepository;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobDemandSnapshotRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Computes and persists composite opportunity score (budget, demand, competition) for a job offer.
 */
@Service
@RequiredArgsConstructor
public class OpportunityScoreService {

    @Value("${jobboard.platform.mock-average-budget:2000}")
    private double mockAverageBudget;

    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final BudgetIntelligenceRepository budgetIntelligenceRepository;
    private final JobDemandSnapshotRepository jobDemandSnapshotRepository;

    @Transactional
    public void computeAndPersist(JobOffer job) {
        if (job.getId() == null) {
            throw new IllegalArgumentException("Job must be persisted before opportunity scoring.");
        }

        double budgetScore = computeBudgetScore(job.getBudgetMax());
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        int demandCount = (int) jobOfferRepository.countPublishedInCategorySince(
                job.getCategory(), JobOfferStatus.PUBLISHED, since);
        double demandScore = 100.0 * (1.0 - Math.exp(-demandCount / 8.0));

        long applicants = jobApplicationRepository.countByJobOfferId(job.getId());
        double competitionScore = 100.0 / (1.0 + applicants);

        double opportunity = budgetScore * 0.40 + demandScore * 0.35 + competitionScore * 0.25;
        opportunity = round2(Math.min(100.0, Math.max(0.0, opportunity)));

        job.setOpportunityScore(opportunity);
        job.setOpportunityBudgetComponent(round2(budgetScore));
        job.setOpportunityDemandComponent(round2(demandScore));
        job.setOpportunityCompetitionComponent(round2(competitionScore));

        BigDecimal avg = BigDecimal.valueOf(mockAverageBudget).setScale(2, RoundingMode.HALF_UP);
        BudgetIntelligence bi = budgetIntelligenceRepository.findByJobOfferId(job.getId())
                .orElseGet(BudgetIntelligence::new);
        bi.setJobOfferId(job.getId());
        bi.setReferenceAverage(avg);
        bi.setBudgetMax(job.getBudgetMax());
        bi.setNormalizedBudgetScore(budgetScore);
        budgetIntelligenceRepository.save(bi);

        jobDemandSnapshotRepository.deleteByJobOfferId(job.getId());
        JobDemandSnapshot snap = new JobDemandSnapshot();
        snap.setJobOfferId(job.getId());
        snap.setCategory(job.getCategory());
        snap.setJobsSameCategory30d(demandCount);
        jobDemandSnapshotRepository.save(snap);
    }

    private double computeBudgetScore(BigDecimal budgetMax) {
        if (budgetMax == null || budgetMax.signum() <= 0) {
            return 0.0;
        }
        double ratio = budgetMax.doubleValue() / mockAverageBudget;
        return round2(Math.min(100.0, ratio * 100.0));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
