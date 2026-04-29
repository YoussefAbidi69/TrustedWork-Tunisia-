package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.dto.*;
import tn.esprit.smartjobboard.entity.JobApplication;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketAnalyticsService")
class MarketAnalyticsServiceTest {

    private List<Object[]> buildList(Object[]... items) { return java.util.Arrays.asList(items); }

    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;

    @InjectMocks
    private MarketAnalyticsService service;

    // ─────────────────────── classifyTrend (static) ───────────────────────

    @Nested
    @DisplayName("classifyTrend()")
    class ClassifyTrend {

        @Test
        @DisplayName("should return RISING when prev=0 and last>0")
        void newSkill() {
            assertThat(MarketAnalyticsService.classifyTrend(0, 5)).isEqualTo(TrendDirection.RISING);
        }

        @Test
        @DisplayName("should return STABLE when both counts are 0")
        void bothZero() {
            assertThat(MarketAnalyticsService.classifyTrend(0, 0)).isEqualTo(TrendDirection.STABLE);
        }

        @Test
        @DisplayName("should return RISING when increase > 20%")
        void risingTrend() {
            assertThat(MarketAnalyticsService.classifyTrend(10, 15)).isEqualTo(TrendDirection.RISING);
        }

        @Test
        @DisplayName("should return DECLINING when decrease > 20%")
        void decliningTrend() {
            assertThat(MarketAnalyticsService.classifyTrend(10, 5)).isEqualTo(TrendDirection.DECLINING);
        }

        @Test
        @DisplayName("should return STABLE when change <= 20%")
        void stableTrend() {
            assertThat(MarketAnalyticsService.classifyTrend(10, 11)).isEqualTo(TrendDirection.STABLE);
        }

        @Test
        @DisplayName("should return STABLE at exactly 20% increase")
        void exactlyTwentyPercent() {
            assertThat(MarketAnalyticsService.classifyTrend(10, 12)).isEqualTo(TrendDirection.STABLE);
        }
    }

    // ─────────────────────── computeMarketInsights() ───────────────────────

    @Nested
    @DisplayName("computeMarketInsights()")
    class ComputeMarketInsights {

        @Test
        @DisplayName("should return empty list when no skills found")
        void noSkills() {
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any())).thenReturn(List.of());

            List<MarketSkillInsightDto> result = service.computeMarketInsights();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should compute insights with correct trend direction")
        void withSkills() {
            List<Object[]> lastWindow = List.of(
                    new Object[]{"java", 15L},
                    new Object[]{"react", 8L}
            );
            List<Object[]> prevWindow = List.of(
                    new Object[]{"java", 10L},
                    new Object[]{"python", 5L}
            );

            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(prevWindow)
                    .thenReturn(lastWindow);

            // Call: first invocation gets prevWindow, second gets lastWindow
            // Need to swap order since the service calls lastWindow first, then prevWindow
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(lastWindow, prevWindow);

            List<MarketSkillInsightDto> result = service.computeMarketInsights();

            assertThat(result).isNotEmpty();
            // Java should be first (highest count in last window)
            assertThat(result.get(0).getSkill()).isEqualTo("java");
        }

        @Test
        @DisplayName("should handle null repository response gracefully")
        void nullResponse() {
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any())).thenReturn(null);

            List<MarketSkillInsightDto> result = service.computeMarketInsights();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should compute change percent correctly")
        void changePercent() {
            List<Object[]> last = buildList(new Object[]{"docker", 20L});
            List<Object[]> prev = buildList(new Object[]{"docker", 10L});

            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(last, prev);

            List<MarketSkillInsightDto> result = service.computeMarketInsights();

            assertThat(result).hasSize(1);
            // (20-10)/10 * 100 = 100%
            assertThat(result.get(0).getChangePercent()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should return 100% change when previous count is 0 and current > 0")
        void newSkillChangePercent() {
            List<Object[]> last = buildList(new Object[]{"rust", 5L});

            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(last, List.of());

            List<MarketSkillInsightDto> result = service.computeMarketInsights();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChangePercent()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should sort by count descending")
        void sortedByCount() {
            List<Object[]> last = List.of(
                    new Object[]{"go", 3L},
                    new Object[]{"java", 50L},
                    new Object[]{"python", 20L}
            );
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(last, List.of());

            List<MarketSkillInsightDto> result = service.computeMarketInsights();

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getSkill()).isEqualTo("java");
            assertThat(result.get(1).getSkill()).isEqualTo("python");
            assertThat(result.get(2).getSkill()).isEqualTo("go");
        }
    }

    // ─────────────────────── topTrendingSkills() ───────────────────────

    @Nested
    @DisplayName("topTrendingSkills()")
    class TopTrending {

        @Test
        @DisplayName("should limit results to N")
        void limitsToN() {
            List<Object[]> last = List.of(
                    new Object[]{"a", 10L},
                    new Object[]{"b", 9L},
                    new Object[]{"c", 8L}
            );
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(last, List.of());

            List<MarketSkillInsightDto> result = service.topTrendingSkills(2);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should handle N=0")
        void zeroN() {
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(buildList(new Object[]{"a", 1L}), List.of());

            List<MarketSkillInsightDto> result = service.topTrendingSkills(0);
            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────── computeSalaryInsights() ───────────────────────

    @Nested
    @DisplayName("computeSalaryInsights()")
    class SalaryInsights {

        @Test
        @DisplayName("should return empty when no applications")
        void noApplications() {
            when(jobApplicationRepository.findAllWithJobOffer()).thenReturn(List.of());

            List<SalaryInsightDto> result = service.computeSalaryInsights();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should compute min/max/avg/median from application rates")
        void withApplications() {
            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setCategory("IT");
            job.setExtractedSkills(List.of("Java"));

            JobApplication app1 = new JobApplication();
            app1.setJobOffer(job);
            app1.setProposedRate(BigDecimal.valueOf(1000));

            JobApplication app2 = new JobApplication();
            app2.setJobOffer(job);
            app2.setProposedRate(BigDecimal.valueOf(2000));

            JobApplication app3 = new JobApplication();
            app3.setJobOffer(job);
            app3.setProposedRate(BigDecimal.valueOf(3000));

            when(jobApplicationRepository.findAllWithJobOffer()).thenReturn(List.of(app1, app2, app3));

            List<SalaryInsightDto> result = service.computeSalaryInsights();

            assertThat(result).hasSize(1);
            SalaryInsightDto insight = result.get(0);
            assertThat(insight.getSkill()).isEqualTo("java");
            assertThat(insight.getMinRate()).isEqualTo(1000.0);
            assertThat(insight.getMaxRate()).isEqualTo(3000.0);
            assertThat(insight.getAvgProposedRate()).isEqualTo(2000.0);
            assertThat(insight.getMedianRate()).isEqualTo(2000.0);
            assertThat(insight.getSampleCount()).isEqualTo(3);
            assertThat(insight.getCategory()).isEqualTo("IT");
        }

        @Test
        @DisplayName("should skip applications with null offer or no extracted skills")
        void nullOfferOrSkills() {
            JobOffer jobNoSkills = new JobOffer();
            jobNoSkills.setId(2L);
            jobNoSkills.setExtractedSkills(List.of());

            JobApplication app1 = new JobApplication();
            app1.setJobOffer(null);
            app1.setProposedRate(BigDecimal.valueOf(1000));

            JobApplication app2 = new JobApplication();
            app2.setJobOffer(jobNoSkills);
            app2.setProposedRate(BigDecimal.valueOf(2000));

            when(jobApplicationRepository.findAllWithJobOffer()).thenReturn(List.of(app1, app2));

            List<SalaryInsightDto> result = service.computeSalaryInsights();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle null proposedRate gracefully")
        void nullRate() {
            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setCategory("IT");
            job.setExtractedSkills(List.of("Python"));

            JobApplication app = new JobApplication();
            app.setJobOffer(job);
            app.setProposedRate(null);

            when(jobApplicationRepository.findAllWithJobOffer()).thenReturn(List.of(app));

            List<SalaryInsightDto> result = service.computeSalaryInsights();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAvgProposedRate()).isEqualTo(0.0);
        }
    }

    // ─────────────────────── computeForecast() ───────────────────────

    @Nested
    @DisplayName("computeForecast()")
    class Forecast {

        @Test
        @DisplayName("should return empty when no skills in current month")
        void noCurrentSkills() {
            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(List.of());

            List<MarketForecastDto> result = service.computeForecast();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should compute forecast with rising slope")
        void risingSlope() {
            // m0=5, m1=10, m2=15 → slope=(15-5)/2=5, next=20, in3months=30
            List<Object[]> m0 = buildList(new Object[]{"java", 5L});
            List<Object[]> m1 = buildList(new Object[]{"java", 10L});
            List<Object[]> m2 = buildList(new Object[]{"java", 15L});

            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(m0, m1, m2);

            List<MarketForecastDto> result = service.computeForecast();

            assertThat(result).hasSize(1);
            MarketForecastDto f = result.get(0);
            assertThat(f.getSkill()).isEqualTo("java");
            assertThat(f.getCurrentDemand()).isEqualTo(15);
            assertThat(f.getForecastNextMonth()).isEqualTo(20);
            assertThat(f.getForecastIn3Months()).isEqualTo(30);
        }

        @Test
        @DisplayName("should clamp forecast to 0 when slope is very negative")
        void negativeSlope() {
            List<Object[]> m0 = buildList(new Object[]{"go", 100L});
            List<Object[]> m1 = buildList(new Object[]{"go", 50L});
            List<Object[]> m2 = buildList(new Object[]{"go", 2L});

            when(jobOfferRepository.countExtractedSkillsBetween(any(), any()))
                    .thenReturn(m0, m1, m2);

            List<MarketForecastDto> result = service.computeForecast();

            assertThat(result).hasSize(1);
            // slope = (2-100)/2 = -49, next = max(0, 2+(-49)) = 0
            assertThat(result.get(0).getForecastNextMonth()).isGreaterThanOrEqualTo(0);
            assertThat(result.get(0).getForecastIn3Months()).isGreaterThanOrEqualTo(0);
        }
    }

    // ─────────────────────── computeOverviewMetrics() ───────────────────────

    @Nested
    @DisplayName("computeOverviewMetrics()")
    class Overview {

        @Test
        @DisplayName("should return zeros when no published jobs")
        void noJobs() {
            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of());

            MarketOverviewDto result = service.computeOverviewMetrics();

            assertThat(result.getActiveJobPostings()).isEqualTo(0);
            assertThat(result.getAvgCompetition()).isEqualTo(0);
        }

        @Test
        @DisplayName("should compute average competition from applications")
        void withApplications() {
            JobOffer job1 = new JobOffer();
            job1.setId(1L);
            JobOffer job2 = new JobOffer();
            job2.setId(2L);

            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of(job1, job2));

            // 3 applications for job1, 1 for job2 → avg = 2.0
            JobApplication a1 = new JobApplication(); a1.setJobOffer(job1);
            JobApplication a2 = new JobApplication(); a2.setJobOffer(job1);
            JobApplication a3 = new JobApplication(); a3.setJobOffer(job1);
            JobApplication a4 = new JobApplication(); a4.setJobOffer(job2);

            when(jobApplicationRepository.findAllWithJobOffer()).thenReturn(List.of(a1, a2, a3, a4));

            MarketOverviewDto result = service.computeOverviewMetrics();

            assertThat(result.getActiveJobPostings()).isEqualTo(2);
            assertThat(result.getAvgCompetition()).isEqualTo(2.0);
        }
    }

    // ─────────────────────── computeCategoriesForSkills() ───────────────────────

    @Nested
    @DisplayName("computeCategoriesForSkills()")
    class CategoriesForSkills {

        @Test
        @DisplayName("should return top categories matching given skills")
        void matchingSkills() {
            JobOffer j1 = new JobOffer();
            j1.setCategory("IT");
            j1.setExtractedSkills(List.of("Java", "Docker"));

            JobOffer j2 = new JobOffer();
            j2.setCategory("Design");
            j2.setExtractedSkills(List.of("Figma"));

            JobOffer j3 = new JobOffer();
            j3.setCategory("IT");
            j3.setExtractedSkills(List.of("Java"));

            when(jobOfferRepository.findByStatusWithExtractedSkills(JobOfferStatus.PUBLISHED))
                    .thenReturn(List.of(j1, j2, j3));

            List<MarketCategoryDto> result = service.computeCategoriesForSkills(List.of("Java"));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo("IT");
            assertThat(result.get(0).getJobCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return all categories when skills list is null")
        void nullSkills() {
            JobOffer j1 = new JobOffer();
            j1.setCategory("IT");
            j1.setExtractedSkills(List.of("Java"));

            when(jobOfferRepository.findByStatusWithExtractedSkills(JobOfferStatus.PUBLISHED))
                    .thenReturn(List.of(j1));

            List<MarketCategoryDto> result = service.computeCategoriesForSkills(null);
            assertThat(result).hasSize(1);
        }
    }
}

