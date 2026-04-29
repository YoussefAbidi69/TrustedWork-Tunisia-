package tn.esprit.smartjobboard.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.smartjobboard.service.MarketAnalyticsService;

/**
 * Periodically recomputes market skill demand so caches/DB consumers stay warm.
 * Insights are computed on read as well; this job keeps JVM paths hot and supports monitoring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledMarketInsightsAggregator {

    private final MarketAnalyticsService marketAnalyticsService;

    @Scheduled(initialDelayString = "${jobboard.market-insights.initial-delay-ms:60000}",
            fixedRateString = "${jobboard.market-insights.interval-ms:1800000}")
    public void aggregateSkillDemand() {
        int n = marketAnalyticsService.computeMarketInsights().size();
        log.info("Market skill demand aggregation tick — {} skill rows", n);
    }
}
