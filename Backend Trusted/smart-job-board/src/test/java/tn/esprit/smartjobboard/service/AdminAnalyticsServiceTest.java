package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.smartjobboard.dto.AdminMarketAnalyticsResponse;
import tn.esprit.smartjobboard.dto.PlatformStatsDto;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.MatchScoreRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsService")
class AdminAnalyticsServiceTest {

    @Mock private MarketAnalyticsService marketAnalyticsService;
    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private MatchScoreRepository matchScoreRepository;

    @InjectMocks
    private AdminAnalyticsService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "mockAverageBudget", 2000.0);
    }

    @Nested
    @DisplayName("platformStats()")
    class PlatformStats {

        @Test
        @DisplayName("should aggregate stats from repositories")
        void aggregateStats() {
            when(jobOfferRepository.count()).thenReturn(100L);
            when(jobOfferRepository.countByStatus(JobOfferStatus.PUBLISHED)).thenReturn(60L);
            when(jobOfferRepository.countByStatus(JobOfferStatus.FLAGGED)).thenReturn(5L);
            when(jobApplicationRepository.count()).thenReturn(250L);
            when(matchScoreRepository.averageTotalScore()).thenReturn(72.5);

            PlatformStatsDto stats = service.platformStats();

            assertThat(stats.getTotalJobs()).isEqualTo(100);
            assertThat(stats.getPublishedJobs()).isEqualTo(60);
            assertThat(stats.getTotalApplications()).isEqualTo(250);
            assertThat(stats.getFlaggedJobs()).isEqualTo(5);
            assertThat(stats.getAvgMatchScore()).isEqualTo(72.5);
        }

        @Test
        @DisplayName("should default avgMatchScore to 0 when no scores exist")
        void noScores() {
            when(jobOfferRepository.count()).thenReturn(0L);
            when(jobOfferRepository.countByStatus(any())).thenReturn(0L);
            when(jobApplicationRepository.count()).thenReturn(0L);
            when(matchScoreRepository.averageTotalScore()).thenReturn(null);

            PlatformStatsDto stats = service.platformStats();

            assertThat(stats.getAvgMatchScore()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("fullReport()")
    class FullReport {

        @Test
        @DisplayName("should compose market insights with published count")
        void composesReport() {
            when(marketAnalyticsService.computeMarketInsights()).thenReturn(List.of());
            when(jobOfferRepository.countByStatus(JobOfferStatus.PUBLISHED)).thenReturn(42L);

            AdminMarketAnalyticsResponse report = service.fullReport();

            assertThat(report.getTotalPublishedJobs()).isEqualTo(42);
            assertThat(report.getPlatformMockAverageBudget()).isEqualTo(2000.0);
            assertThat(report.getSkillInsights()).isEmpty();
        }
    }
}
