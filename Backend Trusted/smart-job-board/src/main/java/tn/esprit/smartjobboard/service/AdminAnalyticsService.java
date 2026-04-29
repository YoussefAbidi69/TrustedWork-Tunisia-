package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.smartjobboard.dto.AdminMarketAnalyticsResponse;
import tn.esprit.smartjobboard.dto.PlatformStatsDto;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MatchScoreRepository;

/**
 * Builds admin-only market analytics payloads backed by real DB aggregates.
 */
@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final MarketAnalyticsService marketAnalyticsService;
    private final JobOfferRepository jobOfferRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MatchScoreRepository matchScoreRepository;

    @Value("${jobboard.platform.mock-average-budget:2000}")
    private double mockAverageBudget;

    @Transactional(readOnly = true)
    public AdminMarketAnalyticsResponse fullReport() {
        return AdminMarketAnalyticsResponse.builder()
                .skillInsights(marketAnalyticsService.computeMarketInsights())
                .totalPublishedJobs(jobOfferRepository.countByStatus(JobOfferStatus.PUBLISHED))
                .platformMockAverageBudget(mockAverageBudget)
                .build();
    }

    @Transactional(readOnly = true)
    public PlatformStatsDto platformStats() {
        Double avg = matchScoreRepository.averageTotalScore();
        return PlatformStatsDto.builder()
                .totalJobs(jobOfferRepository.count())
                .publishedJobs(jobOfferRepository.countByStatus(JobOfferStatus.PUBLISHED))
                .totalApplications(jobApplicationRepository.count())
                .flaggedJobs(jobOfferRepository.countByStatus(JobOfferStatus.FLAGGED))
                .avgMatchScore(avg != null ? avg : 0.0)
                .build();
    }
}
