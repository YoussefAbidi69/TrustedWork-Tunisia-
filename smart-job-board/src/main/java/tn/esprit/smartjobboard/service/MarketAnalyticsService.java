package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.smartjobboard.dto.MarketSkillInsightDto;
import tn.esprit.smartjobboard.dto.TrendDirection;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates published-job skill frequencies with 30-day vs 30-day trend classification.
 */
@Service
@RequiredArgsConstructor
public class MarketAnalyticsService {

    private final JobOfferRepository jobOfferRepository;

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
}
