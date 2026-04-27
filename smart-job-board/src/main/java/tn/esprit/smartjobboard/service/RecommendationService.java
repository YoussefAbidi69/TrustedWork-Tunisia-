package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.JobRecommendationRowDto;
import tn.esprit.smartjobboard.dto.UserReferenceDto;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks published jobs for a freelancer using match, opportunity, and freshness weights.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final JobOfferRepository jobOfferRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final MatchingEngineService matchingEngineService;
    private final CurrentUserService currentUserService;
    private final JobOfferService jobOfferService;

    @Transactional
    public List<JobRecommendationRowDto> recommend(Long freelancerId, List<String> overrideSkills) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        if (me.getRole() == null || !me.getRole().equalsIgnoreCase("FREELANCER")) {
            throw new ForbiddenOperationException("This action requires role FREELANCER.");
        }
        if (!freelancerId.equals(me.getId())) {
            throw new ForbiddenOperationException("You can only request recommendations for your own account.");
        }

        FreelancerProfile fp = freelancerProfileRepository.findByUserId(freelancerId).orElseGet(() -> {
            FreelancerProfile p = new FreelancerProfile();
            p.setUserId(freelancerId);
            p.setEmail(me.getEmail());
            p.setSkills(List.of());
            return p;
        });

        if (overrideSkills != null && !overrideSkills.isEmpty()) {
            List<String> cleaned = overrideSkills.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (!cleaned.isEmpty()) {
                FreelancerProfile overlay = new FreelancerProfile();
                overlay.setUserId(fp.getUserId());
                overlay.setEmail(fp.getEmail());
                overlay.setPreferredRate(fp.getPreferredRate());
                overlay.setSkills(new ArrayList<>(cleaned));
                fp = overlay;
            }
        }

        List<JobOffer> published = jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED);
        List<Ranked> ranked = new ArrayList<>();
        for (JobOffer job : published) {
            BigDecimal rate = rateForRecommendation(job, fp);
            MatchingEngineService.RawMatchEvaluation raw = matchingEngineService.evaluateRaw(job, fp, rate);
            double fresh = freshness(job);
            double rank = (raw.totalScore() / 100.0) * 0.60
                    + (job.getOpportunityScore() / 100.0) * 0.25
                    + fresh * 0.15;
            ranked.add(new Ranked(job, raw, fresh, rank));
        }
        ranked.sort(Comparator.comparingDouble(Ranked::rank).reversed());
        List<Ranked> top = ranked.stream().limit(10).toList();
        final FreelancerProfile fpForRows = fp;
        for (Ranked r : top) {
            matchingEngineService.persistRaw(r.job(), fpForRows, r.raw());
        }

        return top.stream().map(r -> JobRecommendationRowDto.builder()
                .jobOfferId(r.job().getId())
                .title(r.job().getTitle())
                .category(r.job().getCategory())
                .matchScore(r.raw().totalScore())
                .opportunityScore(r.job().getOpportunityScore())
                .freshnessFactor(r.freshness())
                .rankingScore(round4(r.rank()))
                .freshnessScore(r.freshness())
                .recommendationScore(round4(r.rank()))
                .successProbability(r.raw().successProbability())
                .confidence(r.raw().confidence() != null ? r.raw().confidence().name() : "MEDIUM")
                .job(jobOfferService.asResponse(r.job()))
                .topMatchingSkills(topMatchingSkills(r.job(), fpForRows))
                .build()
        ).toList();
    }

    private List<String> topMatchingSkills(JobOffer job, FreelancerProfile fp) {
        List<String> need = matchingEngineService.mergeJobSkills(job);
        List<String> have = fp.getSkills() == null ? List.of() : fp.getSkills();
        List<String> out = new ArrayList<>();
        for (String n : need) {
            if (n == null || n.isBlank()) {
                continue;
            }
            for (String h : have) {
                if (h != null && n.trim().equalsIgnoreCase(h.trim())) {
                    out.add(n.trim());
                    break;
                }
            }
            if (out.size() >= 8) {
                break;
            }
        }
        return out;
    }

    private static BigDecimal rateForRecommendation(JobOffer job, FreelancerProfile fp) {
        if (fp.getPreferredRate() != null) {
            return fp.getPreferredRate();
        }
        // Fallback to a fixed market average if the user has no preferred rate,
        // so that the budgetFit naturally varies across different jobs instead of always being 100%.
        return BigDecimal.valueOf(800.00);
    }

    private static double freshness(JobOffer job) {
        if (job.getPublishedAt() == null) {
            return 0.2;
        }
        LocalDate pub = job.getPublishedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(pub, today);
        if (days <= 0) {
            return 1.0;
        }
        if (days <= 7) {
            return 0.5;
        }
        return 0.2;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private record Ranked(JobOffer job, MatchingEngineService.RawMatchEvaluation raw, double freshness, double rank) {
    }
}
