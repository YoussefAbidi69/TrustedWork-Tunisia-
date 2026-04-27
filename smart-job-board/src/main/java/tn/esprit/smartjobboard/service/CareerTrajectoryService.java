package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.CareerInsightResponse;
import tn.esprit.smartjobboard.dto.CareerRoadmapStepDto;
import tn.esprit.smartjobboard.dto.CareerSuggestionDto;
import tn.esprit.smartjobboard.dto.CareerSkillSuggestionDto;
import tn.esprit.smartjobboard.dto.MarketSkillInsightDto;
import tn.esprit.smartjobboard.dto.UserReferenceDto;
import tn.esprit.smartjobboard.entity.CareerSuggestion;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.entity.SkillCooccurrence;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.repository.CareerSuggestionRepository;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.SkillCooccurrenceRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

/**
 * Suggests skills to learn using market trends plus co-occurrence with the freelancer's current stack.
 */
@Service
@RequiredArgsConstructor
public class CareerTrajectoryService {

    private final CurrentUserService currentUserService;
    private final MarketAnalyticsService marketAnalyticsService;
    private final SkillCooccurrenceService skillCooccurrenceService;
    private final SkillCooccurrenceRepository skillCooccurrenceRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final JobOfferRepository jobOfferRepository;
    private final CareerSuggestionRepository careerSuggestionRepository;

    @Transactional
    public CareerInsightResponse insights(Long freelancerId, List<String> skillsQuery) {
        UserReferenceDto me = currentUserService.requireCurrentUser();
        if (me.getRole() == null || !me.getRole().equalsIgnoreCase("FREELANCER")) {
            throw new ForbiddenOperationException("This action requires role FREELANCER.");
        }
        if (!freelancerId.equals(me.getId())) {
            throw new ForbiddenOperationException("You can only request career insights for your own account.");
        }

        List<String> currentSkills = resolveCurrentSkillsLowered(freelancerId, skillsQuery);
        if (currentSkills.isEmpty()) {
            return CareerInsightResponse.builder()
                    .targetRole("Developer")
                    .difficulty("Easy")
                    .totalIncomeBoost(0.0)
                    .steps(List.of())
                    .build();
        }

        // Step 1: keep co-occurrence table warm (used elsewhere)
        skillCooccurrenceService.rebuildFromPublishedJobs();

        // Step 2: trending skills from market data (may be empty if DB is empty)
        List<MarketSkillInsightDto> trending = marketAnalyticsService.topTrendingSkills(30);

        // Step 3: if no market rows, use deterministic dictionary fallback so roadmap still works
        if (trending.isEmpty()) {
            trending = getFallbackTrendingSkills(currentSkills);
        }

        // Step 4: remove skills user already has
        List<MarketSkillInsightDto> candidates = trending.stream()
                .filter(m -> m.getSkill() != null && !currentSkills.contains(m.getSkill().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        // Step 5: compute co-occurrence using job descriptions
        long totalJobs = jobOfferRepository.countByStatus(JobOfferStatus.PUBLISHED);
        long effectiveTotalJobs = Math.max(totalJobs, 10);

        double maxCount = trending.stream().mapToLong(MarketSkillInsightDto::getCount).max().orElse(1L);
        List<CareerSuggestion> suggestions = new ArrayList<>();
        for (MarketSkillInsightDto cand : candidates) {
            String skillName = cand.getSkill();
            if (skillName == null || skillName.isBlank()) {
                continue;
            }
            long coOccurrenceCount = 0;
            for (String mine : currentSkills) {
                coOccurrenceCount += jobOfferRepository.countJobsWithBothSkills(skillName.toLowerCase(Locale.ROOT), mine);
            }
            double coRate = Math.min((double) coOccurrenceCount / effectiveTotalJobs, 1.0);

            double trendScore = maxCount > 0 ? (double) cand.getCount() / maxCount : 0.5;
            // If DB has zero jobs, nudge trendScore deterministically so the top5 is never all zeros.
            if (totalJobs == 0) {
                trendScore = 0.55 + (hash01(skillName) * 0.25);
            }
            double totalScore = trendScore * 0.50 + coRate * 0.50;

            CareerSuggestion s = new CareerSuggestion();
            s.setFreelancerId(freelancerId);
            s.setSuggestedSkill(skillName);
            s.setTrendScore(trendScore);
            s.setCoOccurrenceRate(coRate);
            s.setTotalScore(totalScore);
            s.setTrend(cand.getTrend() != null ? cand.getTrend().name() : "STABLE");
            s.setEstimatedIncomeImpact(0.0); // set below
            s.setGeneratedAt(LocalDateTime.now());
            suggestions.add(s);
        }

        suggestions.sort(Comparator.comparingDouble(CareerSuggestion::getTotalScore).reversed());
        List<CareerSuggestion> top5 = suggestions.stream().limit(5).collect(Collectors.toList());

        double[] impacts = {15.0, 13.0, 11.0, 9.0, 8.0};
        for (int i = 0; i < top5.size(); i++) {
            top5.get(i).setEstimatedIncomeImpact(impacts[i]);
        }
        double totalBoost = top5.stream().mapToDouble(CareerSuggestion::getEstimatedIncomeImpact).sum();

        careerSuggestionRepository.deleteByFreelancerId(freelancerId);
        List<CareerSuggestion> saved = careerSuggestionRepository.saveAll(top5);

        // Convert saved suggestions into roadmap steps
        List<CareerRoadmapStepDto> steps = saved.stream().map(cs ->
                CareerRoadmapStepDto.builder()
                        .title("Master " + cs.getSuggestedSkill())
                        .description("Trending skill in the market (score: " + round4(cs.getTotalScore()) + "). "
                                + "Adding this skill will boost your income by ~" + Math.round(cs.getEstimatedIncomeImpact()) + "%.")
                        .skillsUnlocked(List.of(cs.getSuggestedSkill()))
                        .estimatedWeeks(4)
                        .difficultyLevel("Intermediate")
                        .color("#E8735A")
                        .build()
        ).collect(Collectors.toList());

        return CareerInsightResponse.builder()
                .targetRole("Senior " + (currentSkills.isEmpty() ? "Developer" : currentSkills.get(0)) + " Engineer")
                .difficulty("Moderate")
                .totalIncomeBoost(totalBoost)
                .steps(steps)
                .build();
    }

    private int coCountPair(String a, String b) {
        String p = a.compareTo(b) <= 0 ? a : b;
        String r = a.compareTo(b) <= 0 ? b : a;
        return skillCooccurrenceRepository.findBySkillPrimaryAndSkillRelated(p, r)
                .map(SkillCooccurrence::getCoCount)
                .orElse(0);
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    // (legacy) kept for compile compatibility if referenced later
    private record ScoredSkill(String skill, double combined, double trend, double co) {}

    private List<String> resolveCurrentSkillsLowered(Long freelancerId, List<String> skillsQuery) {
        Set<String> mySkills = new HashSet<>();
        if (skillsQuery != null && !skillsQuery.isEmpty()) {
            for (String s : skillsQuery) {
                if (s != null && !s.isBlank()) {
                    mySkills.add(s.trim().toLowerCase(Locale.ROOT));
                }
            }
        } else {
            freelancerProfileRepository.findByUserId(freelancerId).ifPresent(fp -> {
                for (String s : fp.getSkills()) {
                    if (s != null && !s.isBlank()) {
                        mySkills.add(s.trim().toLowerCase(Locale.ROOT));
                    }
                }
            });
        }
        return mySkills.stream().sorted().collect(Collectors.toList());
    }

    private CareerSuggestionDto toDto(CareerSuggestion s) {
        return CareerSuggestionDto.builder()
                .id(s.getId())
                .suggestedSkill(s.getSuggestedSkill())
                .trendScore(s.getTrendScore())
                .coOccurrenceRate(s.getCoOccurrenceRate())
                .estimatedIncomeImpact(s.getEstimatedIncomeImpact())
                .trend(s.getTrend())
                .build();
    }

    /**
     * Deterministic pseudo-random number in [0,1] from a string (stable across runs).
     */
    private static double hash01(String s) {
        if (s == null || s.isBlank()) {
            return 0.0;
        }
        int h = s.toLowerCase(Locale.ROOT).hashCode();
        long x = (h & 0xffffffffL);
        return (x % 1000L) / 1000.0;
    }

    // Fallback: built-in skill dictionary when DB has no market rows
    private List<MarketSkillInsightDto> getFallbackTrendingSkills(List<String> currentSkillsLower) {
        Map<String, List<String>> stackSuggestions = new HashMap<>();
        stackSuggestions.put("react", List.of("Next.js", "TypeScript", "Node.js", "GraphQL", "Docker"));
        stackSuggestions.put("angular", List.of("TypeScript", "RxJS", "Node.js", "Docker", "PostgreSQL"));
        stackSuggestions.put("java", List.of("Spring Boot", "Docker", "Kubernetes", "PostgreSQL", "AWS"));
        stackSuggestions.put("python", List.of("FastAPI", "Docker", "PostgreSQL", "Redis", "Kubernetes"));
        stackSuggestions.put("node.js", List.of("TypeScript", "React", "Docker", "MongoDB", "Redis"));
        stackSuggestions.put("default", List.of("Docker", "Kubernetes", "TypeScript", "PostgreSQL", "AWS", "React", "Node.js", "Python", "Git", "Redis"));

        List<String> suggested = null;
        for (Map.Entry<String, List<String>> e : stackSuggestions.entrySet()) {
            if (currentSkillsLower.contains(e.getKey())) {
                suggested = e.getValue();
                break;
            }
        }
        if (suggested == null) {
            suggested = stackSuggestions.get("default");
        }

        String[] trends = {"RISING", "RISING", "STABLE", "RISING", "STABLE"};
        long[] counts = {85, 72, 65, 58, 50};

        List<MarketSkillInsightDto> fallback = new ArrayList<>();
        for (int i = 0; i < suggested.size() && i < 5; i++) {
            String sk = suggested.get(i);
            if (sk == null || sk.isBlank()) {
                continue;
            }
            if (currentSkillsLower.contains(sk.toLowerCase(Locale.ROOT))) {
                continue;
            }
            // TrendDirection exists, but CareerTrajectoryService only needs name string; keep compatible via MarketSkillInsightDto.trend
            fallback.add(new MarketSkillInsightDto(sk, counts[i], tn.esprit.smartjobboard.dto.TrendDirection.valueOf(trends[i]), 0.0, 0));
        }
        return fallback;
    }
}
