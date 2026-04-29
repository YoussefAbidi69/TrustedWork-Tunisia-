package tn.esprit.smartjobboard.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.OpportunityNotificationLogRepository;
import tn.esprit.smartjobboard.service.MatchingEngineService;
import tn.esprit.smartjobboard.service.OpportunityNotificationDispatcher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hourly scan of freshly published jobs; emails the top five matching freelancers and marks jobs as processed.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class ScheduledOpportunityAgent {

    private final JobOfferRepository jobOfferRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final MatchingEngineService matchingEngineService;
    private final OpportunityNotificationDispatcher opportunityNotificationDispatcher;
    private final OpportunityNotificationLogRepository opportunityNotificationLogRepository;

    @Scheduled(fixedRateString = "${jobboard.agent.interval-ms:3600000}")
    @Transactional
    public void runHourly() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(60);
        List<JobOffer> fresh = jobOfferRepository
                .findByStatusAndPublishedAtAfterAndOpportunityAgentProcessedAtIsNull(JobOfferStatus.PUBLISHED, since);
        log.info("Opportunity agent: {} fresh published job(s) in the last 60 minutes.", fresh.size());
        for (JobOffer job : fresh) {
            try {
                dispatchForJob(job);
            } catch (Exception e) {
                log.error("Opportunity agent failed for job {}", job.getId(), e);
            } finally {
                job.setOpportunityAgentProcessedAt(LocalDateTime.now());
                jobOfferRepository.save(job);
            }
        }
    }

    private void dispatchForJob(JobOffer job) {
        List<FreelancerProfile> profiles = freelancerProfileRepository.findAllWithSkills();
        List<Ranked> ranked = new ArrayList<>();
        for (FreelancerProfile fp : profiles) {
            BigDecimal rate = fp.getPreferredRate() != null
                    ? fp.getPreferredRate()
                    : job.getBudgetMin().add(job.getBudgetMax()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            MatchingEngineService.RawMatchEvaluation raw = matchingEngineService.evaluateRaw(job, fp, rate);
            ranked.add(new Ranked(fp, raw.totalScore()));
        }
        ranked.sort(Comparator.comparingDouble(Ranked::score).reversed());
        List<Ranked> top = ranked.stream().limit(5).toList();
        for (Ranked r : top) {
            if (opportunityNotificationLogRepository.existsByJobOfferIdAndFreelancerId(job.getId(), r.profile().getUserId())) {
                continue;
            }
            opportunityNotificationDispatcher.notifyFreelancer(job, r.profile(), r.score());
        }
    }

    private record Ranked(FreelancerProfile profile, double score) {
    }
}
