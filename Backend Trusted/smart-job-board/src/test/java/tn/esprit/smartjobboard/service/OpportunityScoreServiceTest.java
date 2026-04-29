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
import tn.esprit.smartjobboard.entity.BudgetIntelligence;
import tn.esprit.smartjobboard.entity.JobDemandSnapshot;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.BudgetIntelligenceRepository;
import tn.esprit.smartjobboard.repository.JobApplicationRepository;
import tn.esprit.smartjobboard.repository.JobDemandSnapshotRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpportunityScoreService")
class OpportunityScoreServiceTest {

    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private BudgetIntelligenceRepository budgetIntelligenceRepository;
    @Mock private JobDemandSnapshotRepository jobDemandSnapshotRepository;

    @InjectMocks
    private OpportunityScoreService service;

    private JobOffer job;

    @BeforeEach
    void setUp() {
        // Default mock average budget used in budget score calculation
        ReflectionTestUtils.setField(service, "mockAverageBudget", 2000.0);

        job = new JobOffer();
        job.setId(1L);
        job.setClientId(10L);
        job.setCategory("IT");
        job.setBudgetMax(BigDecimal.valueOf(2000));
        job.setStatus(JobOfferStatus.PUBLISHED);
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw if job has no ID (not persisted)")
        void unpersistedJob_throws() {
            job.setId(null);
            assertThatThrownBy(() -> service.computeAndPersist(job))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("persisted");
        }
    }

    @Nested
    @DisplayName("Budget score computation")
    class BudgetScore {

        @Test
        @DisplayName("should compute 100% budget score when budget equals average")
        void budgetEqualsAverage() {
            job.setBudgetMax(BigDecimal.valueOf(2000));
            stubRepos(0, 0);

            service.computeAndPersist(job);

            // budgetScore = min(100, (2000/2000)*100) = 100
            assertThat(job.getOpportunityBudgetComponent()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should compute 50% budget score when budget is half of average")
        void budgetHalfOfAverage() {
            job.setBudgetMax(BigDecimal.valueOf(1000));
            stubRepos(0, 0);

            service.computeAndPersist(job);

            assertThat(job.getOpportunityBudgetComponent()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("should cap budget score at 100 when budget exceeds average")
        void budgetExceedsAverage() {
            job.setBudgetMax(BigDecimal.valueOf(5000));
            stubRepos(0, 0);

            service.computeAndPersist(job);

            assertThat(job.getOpportunityBudgetComponent()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("should return 0 budget score for null budget")
        void nullBudget() {
            job.setBudgetMax(null);
            stubRepos(0, 0);

            service.computeAndPersist(job);

            assertThat(job.getOpportunityBudgetComponent()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 budget score for zero budget")
        void zeroBudget() {
            job.setBudgetMax(BigDecimal.ZERO);
            stubRepos(0, 0);

            service.computeAndPersist(job);

            assertThat(job.getOpportunityBudgetComponent()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Demand score")
    class DemandScore {

        @Test
        @DisplayName("should be near 0 when no other jobs in category")
        void noDemand() {
            stubRepos(0, 0);

            service.computeAndPersist(job);

            // demandScore = 100 * (1 - exp(0/8)) = 100 * (1-1) = 0
            assertThat(job.getOpportunityDemandComponent()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should increase with more jobs in category")
        void highDemand() {
            stubRepos(20, 0);

            service.computeAndPersist(job);

            // demandScore = 100 * (1 - exp(-20/8)) ≈ 91.8
            assertThat(job.getOpportunityDemandComponent()).isGreaterThan(90.0);
        }
    }

    @Nested
    @DisplayName("Competition score")
    class CompetitionScore {

        @Test
        @DisplayName("should be 100 when no applicants")
        void noApplicants() {
            stubRepos(0, 0);

            service.computeAndPersist(job);

            // competitionScore = 100 / (1 + 0) = 100
            assertThat(job.getOpportunityCompetitionComponent()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should decrease with more applicants")
        void manyApplicants() {
            stubRepos(0, 9);

            service.computeAndPersist(job);

            // competitionScore = 100 / (1 + 9) = 10
            assertThat(job.getOpportunityCompetitionComponent()).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("Composite opportunity score")
    class CompositeScore {

        @Test
        @DisplayName("should combine all three components correctly")
        void compositeCalculation() {
            // budget = 2000/2000*100 = 100
            // demand ≈ 0 (no jobs)
            // competition = 100/(1+0) = 100
            // total = 100*0.4 + 0*0.35 + 100*0.25 = 40 + 0 + 25 = 65
            stubRepos(0, 0);

            service.computeAndPersist(job);

            assertThat(job.getOpportunityScore()).isCloseTo(65.0, within(0.5));
        }

        @Test
        @DisplayName("should clamp score between 0 and 100")
        void clampedScore() {
            job.setBudgetMax(BigDecimal.valueOf(10000));
            stubRepos(50, 0);

            service.computeAndPersist(job);

            assertThat(job.getOpportunityScore()).isLessThanOrEqualTo(100.0);
            assertThat(job.getOpportunityScore()).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Persistence side effects")
    class Persistence {

        @Test
        @DisplayName("should persist BudgetIntelligence record")
        void budgetIntelligencePersisted() {
            stubRepos(0, 0);

            service.computeAndPersist(job);

            verify(budgetIntelligenceRepository).save(any(BudgetIntelligence.class));
        }

        @Test
        @DisplayName("should persist JobDemandSnapshot")
        void demandSnapshotPersisted() {
            stubRepos(0, 0);

            service.computeAndPersist(job);

            verify(jobDemandSnapshotRepository).deleteByJobOfferId(1L);
            verify(jobDemandSnapshotRepository).save(any(JobDemandSnapshot.class));
        }
    }

    /**
     * Helper: stubs repo queries.
     * @param categoryCount number of published jobs in the same category
     * @param applicantCount number of applicants for this job
     */
    private void stubRepos(long categoryCount, long applicantCount) {
        when(jobOfferRepository.countPublishedInCategorySince(anyString(), any(JobOfferStatus.class), any()))
                .thenReturn(categoryCount);
        when(jobApplicationRepository.countByJobOfferId(1L)).thenReturn(applicantCount);
        when(budgetIntelligenceRepository.findByJobOfferId(1L)).thenReturn(Optional.empty());
    }
}
