package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.smartjobboard.dto.MarketCategoryDto;
import tn.esprit.smartjobboard.dto.MarketForecastDto;
import tn.esprit.smartjobboard.dto.MarketOverviewDto;
import tn.esprit.smartjobboard.dto.MarketSkillInsightDto;
import tn.esprit.smartjobboard.dto.SalaryInsightDto;
import tn.esprit.smartjobboard.dto.TrendDirection;
import tn.esprit.smartjobboard.entity.JobApplication;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregates published-job skill frequencies with 30-day vs 30-day trend classification.
 */
@Service
@RequiredArgsConstructor
public class MarketAnalyticsService {

    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository jobApplicationRepository;

    /**
     * Explicit refresh hook (admin + scheduler): recomputes the same snapshot as reads.
     */
    public void aggregateSkillDemand() {
        computeMarketInsights();
    }

    public List<MarketSkillInsightDto> computeMarketInsights() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startLast = now.minusDays(30);
        LocalDateTime startPrev = now.minusDays(60);

        Map<String, Long> lastWindow = toMap(jobOfferRepository.countExtractedSkillsBetween(startLast, now));
        Map<String, Long> prevWindow = toMap(jobOfferRepository.countExtractedSkillsBetween(startPrev, startLast));

        Set<String> all = new HashSet<>();
        all.addAll(lastWindow.keySet());
        all.addAll(prevWindow.keySet());

        List<MarketSkillInsightDto> rows = new ArrayList<>();
        for (String skill : all) {
            long c1 = lastWindow.getOrDefault(skill, 0L);
            long c0 = prevWindow.getOrDefault(skill, 0L);
            TrendDirection trend = classifyTrend(c0, c1);
            double changePercent = c0 == 0 ? (c1 > 0 ? 100.0 : 0.0)
                    : Math.round(1000.0 * (c1 - (double) c0) / c0) / 10.0;
            rows.add(new MarketSkillInsightDto(skill, c1, trend, changePercent, c0));
        }
        rows.sort(Comparator.comparingLong(MarketSkillInsightDto::getCount).reversed());
        return rows;
    }

    private static Map<String, Long> toMap(List<Object[]> raw) {
        Map<String, Long> m = new HashMap<>();
        if (raw == null) {
            return m;
        }
        for (Object[] row : raw) {
            if (row == null || row.length < 2) {
                continue;
            }
            String sk = String.valueOf(row[0]).toLowerCase(Locale.ROOT);
            long cnt = ((Number) row[1]).longValue();
            m.put(sk, cnt);
        }
        return m;
    }

    static TrendDirection classifyTrend(long prevCount, long lastCount) {
        if (prevCount == 0) {
            if (lastCount > 0) {
                return TrendDirection.RISING;
            }
            return TrendDirection.STABLE;
        }
        double change = (lastCount - (double) prevCount) / prevCount;
        if (change > 0.20) {
            return TrendDirection.RISING;
        }
        if (change < -0.20) {
            return TrendDirection.DECLINING;
        }
        return TrendDirection.STABLE;
    }

    /**
     * Top N skills by recent-window count for career heuristics.
     */
    public List<MarketSkillInsightDto> topTrendingSkills(int n) {
        List<MarketSkillInsightDto> all = computeMarketInsights();
        return all.stream().limit(Math.max(0, n)).toList();
    }

    public List<SalaryInsightDto> computeSalaryInsights() {
        List<JobApplication> applications = jobApplicationRepository.findAllWithJobOffer();
        Map<String, List<Double>> ratesBySkill = new HashMap<>();
        Map<String, List<String>> categoriesBySkill = new HashMap<>();

        for (JobApplication application : applications) {
            JobOffer offer = application.getJobOffer();
            if (offer == null || offer.getExtractedSkills() == null || offer.getExtractedSkills().isEmpty()) {
                continue;
            }
            double rate = safeDouble(application.getProposedRate());
            String category = offer.getCategory() == null ? "Unknown" : offer.getCategory();
            for (String rawSkill : offer.getExtractedSkills()) {
                String skill = normalizeSkill(rawSkill);
                if (skill.isBlank()) continue;
                ratesBySkill.computeIfAbsent(skill, k -> new ArrayList<>()).add(rate);
                categoriesBySkill.computeIfAbsent(skill, k -> new ArrayList<>()).add(category);
            }
        }

        List<SalaryInsightDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Double>> entry : ratesBySkill.entrySet()) {
            List<Double> rates = entry.getValue();
            if (rates.isEmpty()) continue;
            rates.sort(Double::compareTo);
            double min = rates.get(0);
            double max = rates.get(rates.size() - 1);
            double avg = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double median = median(rates);
            String topCategory = mostFrequent(categoriesBySkill.getOrDefault(entry.getKey(), List.of()));
            result.add(SalaryInsightDto.builder()
                    .skill(entry.getKey())
                    .avgProposedRate(round2(avg))
                    .minRate(round2(min))
                    .maxRate(round2(max))
                    .medianRate(round2(median))
                    .sampleCount(rates.size())
                    .category(topCategory)
                    .build());
        }
        result.sort(Comparator.comparingDouble(SalaryInsightDto::getMedianRate).reversed());
        return result;
    }

    public List<MarketForecastDto> computeForecast() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);

        Map<String, Long> m0 = toMap(jobOfferRepository.countExtractedSkillsBetween(
                twoMonthsAgo.atDay(1).atStartOfDay(), previousMonth.atDay(1).atStartOfDay()));
        Map<String, Long> m1 = toMap(jobOfferRepository.countExtractedSkillsBetween(
                previousMonth.atDay(1).atStartOfDay(), currentMonth.atDay(1).atStartOfDay()));
        Map<String, Long> m2 = toMap(jobOfferRepository.countExtractedSkillsBetween(
                currentMonth.atDay(1).atStartOfDay(), now));

        List<String> topSkills = m2.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        List<MarketForecastDto> result = new ArrayList<>();
        for (String skill : topSkills) {
            long c0 = m0.getOrDefault(skill, 0L);
            long c1 = m1.getOrDefault(skill, 0L);
            long c2 = m2.getOrDefault(skill, 0L);
            double slope = (c2 - c0) / 2.0;
            long nextMonth = Math.max(0L, Math.round(c2 + slope));
            long in3Months = Math.max(0L, Math.round(c2 + (3 * slope)));
            String confidence = confidenceFor(c0, c1, c2);

            result.add(MarketForecastDto.builder()
                    .skill(skill)
                    .currentDemand(c2)
                    .forecastNextMonth(nextMonth)
                    .forecastIn3Months(in3Months)
                    .confidence(confidence)
                    .build());
        }
        return result;
    }

    public List<MarketCategoryDto> computeCategoriesForSkills(List<String> skills) {
        List<JobOffer> offers = jobOfferRepository.findByStatusWithExtractedSkills(JobOfferStatus.PUBLISHED);
        Set<String> normalizedSkills = (skills == null ? List.<String>of() : skills).stream()
                .map(this::normalizeSkill)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        Map<String, Long> categories = new HashMap<>();
        for (JobOffer offer : offers) {
            if (offer.getCategory() == null || offer.getCategory().isBlank()) continue;
            if (!normalizedSkills.isEmpty()) {
                Set<String> offerSkills = offer.getExtractedSkills() == null
                        ? Set.of()
                        : offer.getExtractedSkills().stream().map(this::normalizeSkill).collect(Collectors.toSet());
                boolean matches = normalizedSkills.stream().anyMatch(offerSkills::contains);
                if (!matches) continue;
            }
            categories.merge(offer.getCategory().trim(), 1L, Long::sum);
        }
        return categories.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> MarketCategoryDto.builder().category(e.getKey()).jobCount(e.getValue()).build())
                .toList();
    }

    public MarketOverviewDto computeOverviewMetrics() {
        List<JobOffer> publishedJobs = jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED);
        long activeJobPostings = publishedJobs.size();
        if (activeJobPostings == 0) {
            return new MarketOverviewDto(0, 0);
        }
        Map<Long, Long> appCountByJob = jobApplicationRepository.findAllWithJobOffer().stream()
                .collect(Collectors.groupingBy(a -> a.getJobOffer().getId(), Collectors.counting()));

        double avgCompetition = publishedJobs.stream()
                .mapToLong(j -> appCountByJob.getOrDefault(j.getId(), 0L))
                .average()
                .orElse(0.0);

        return MarketOverviewDto.builder()
                .activeJobPostings(activeJobPostings)
                .avgCompetition(round2(avgCompetition))
                .build();
    }

    private String confidenceFor(long c0, long c1, long c2) {
        long sum = c0 + c1 + c2;
        long variation = Math.abs(c2 - c1) + Math.abs(c1 - c0);
        if (sum >= 60 && variation <= Math.max(3, sum / 8)) return "HIGH";
        if (sum >= 20) return "MEDIUM";
        return "LOW";
    }

    private String mostFrequent(List<String> values) {
        if (values == null || values.isEmpty()) return "Unknown";
        Map<String, Long> freq = new LinkedHashMap<>();
        for (String value : values) {
            String key = value == null || value.isBlank() ? "Unknown" : value;
            freq.merge(key, 1L, Long::sum);
        }
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }

    private String normalizeSkill(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private double safeDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        int mid = size / 2;
        if (size % 2 == 0) {
            return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        }
        return sorted.get(mid);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
