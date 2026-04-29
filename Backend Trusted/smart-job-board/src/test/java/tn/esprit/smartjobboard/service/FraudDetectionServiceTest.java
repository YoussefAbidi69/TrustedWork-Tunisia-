package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.nullable;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService")
class FraudDetectionServiceTest {

    @Mock
    private JobOfferRepository jobOfferRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private JobOffer job;

    @BeforeEach
    void setUp() {
        job = new JobOffer();
        job.setId(1L);
        job.setClientId(10L);
        job.setTitle("Senior Java Developer for E-Commerce Platform");
        job.setDescription("We are looking for a senior Java developer with experience in Spring Boot, "
                + "microservices architecture, and cloud deployments. The role involves designing APIs, "
                + "mentoring junior developers, and ensuring code quality through reviews.");
        job.setBudgetMax(BigDecimal.valueOf(5000));
        job.setRequiredSkills(List.of("Java", "Spring Boot", "Docker"));
        job.setStatus(JobOfferStatus.DRAFT);
    }

    @Nested
    @DisplayName("Clean job offers")
    class CleanOffers {

        @Test
        @DisplayName("should return zero fraud score for a well-formed job")
        void wellFormedJob_zeroFraudScore() {
            when(jobOfferRepository.countByClientIdAndCreatedAtAfter(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(1L);
            when(jobOfferRepository.countDuplicateTitleSince(eq(10L), anyString(), any(LocalDateTime.class), eq(1L)))
                    .thenReturn(0L);

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.fraudRiskScore()).isEqualTo(0.0);
            assertThat(result.triggers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Low budget signal")
    class LowBudget {

        @Test
        @DisplayName("should trigger LOW_BUDGET when budgetMax < 50")
        void lowBudget_triggers() {
            job.setBudgetMax(BigDecimal.valueOf(30));
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.fraudRiskScore()).isCloseTo(0.30, within(0.001));
            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("LOW_BUDGET");
        }

        @Test
        @DisplayName("should NOT trigger when budgetMax = 50")
        void budgetExactly50_noTrigger() {
            job.setBudgetMax(BigDecimal.valueOf(50));
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .doesNotContain("LOW_BUDGET");
        }
    }

    @Nested
    @DisplayName("Short description signal")
    class ShortDescription {

        @Test
        @DisplayName("should trigger SHORT_DESCRIPTION when < 100 chars")
        void shortDesc_triggers() {
            job.setDescription("Short job.");
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("SHORT_DESCRIPTION");
        }

        @Test
        @DisplayName("should trigger SHORT_DESCRIPTION when description is null")
        void nullDesc_triggers() {
            job.setDescription(null);
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("SHORT_DESCRIPTION");
        }
    }

    @Nested
    @DisplayName("Short title signal")
    class ShortTitle {

        @Test
        @DisplayName("should trigger SHORT_TITLE when title < 10 chars")
        void shortTitle_triggers() {
            job.setTitle("Dev");
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("SHORT_TITLE");
        }

        @Test
        @DisplayName("should trigger SHORT_TITLE when title is null")
        void nullTitle_triggers() {
            job.setTitle(null);
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("SHORT_TITLE");
        }
    }

    @Nested
    @DisplayName("High posting velocity signal")
    class HighVelocity {

        @Test
        @DisplayName("should trigger HIGH_POSTING_VELOCITY when > 5 jobs in 24h")
        void highVelocity_triggers() {
            when(jobOfferRepository.countByClientIdAndCreatedAtAfter(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(8L);
            when(jobOfferRepository.countDuplicateTitleSince(eq(10L), anyString(), any(LocalDateTime.class), eq(1L)))
                    .thenReturn(0L);

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("HIGH_POSTING_VELOCITY");
        }

        @Test
        @DisplayName("should NOT trigger when exactly 5 jobs in 24h")
        void exactlyFive_noTrigger() {
            when(jobOfferRepository.countByClientIdAndCreatedAtAfter(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(5L);
            when(jobOfferRepository.countDuplicateTitleSince(eq(10L), anyString(), any(LocalDateTime.class), eq(1L)))
                    .thenReturn(0L);

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .doesNotContain("HIGH_POSTING_VELOCITY");
        }
    }

    @Nested
    @DisplayName("Duplicate title signal")
    class DuplicateTitle {

        @Test
        @DisplayName("should trigger DUPLICATE_TITLE when same title exists within 7 days")
        void dupTitle_triggers() {
            when(jobOfferRepository.countByClientIdAndCreatedAtAfter(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(1L);
            when(jobOfferRepository.countDuplicateTitleSince(eq(10L), anyString(), any(LocalDateTime.class), eq(1L)))
                    .thenReturn(2L);

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("DUPLICATE_TITLE");
        }
    }

    @Nested
    @DisplayName("No required skills signal")
    class NoSkills {

        @Test
        @DisplayName("should trigger NO_REQUIRED_SKILLS when skills list is null")
        void nullSkills_triggers() {
            job.setRequiredSkills(null);
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("NO_REQUIRED_SKILLS");
        }

        @Test
        @DisplayName("should trigger NO_REQUIRED_SKILLS when all skills are blank")
        void blankSkills_triggers() {
            job.setRequiredSkills(List.of("", "  "));
            stubNoOtherSignals();

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.triggers()).extracting(FraudDetectionService.TriggeredSignal::code)
                    .contains("NO_REQUIRED_SKILLS");
        }
    }

    @Nested
    @DisplayName("Score capping")
    class ScoreCapping {

        @Test
        @DisplayName("should cap fraud score at 1.0 when multiple signals fire")
        void multipleSignals_capAtOne() {
            job.setTitle("x");
            job.setDescription("bad");
            job.setBudgetMax(BigDecimal.valueOf(10));
            job.setRequiredSkills(null);
            when(jobOfferRepository.countByClientIdAndCreatedAtAfter(eq(10L), any(LocalDateTime.class)))
                    .thenReturn(20L);
            when(jobOfferRepository.countDuplicateTitleSince(eq(10L), anyString(), any(LocalDateTime.class), eq(1L)))
                    .thenReturn(3L);

            FraudDetectionService.FraudAssessment result = fraudDetectionService.assess(job, 1L);

            assertThat(result.fraudRiskScore()).isLessThanOrEqualTo(1.0);
            assertThat(result.triggers().size()).isGreaterThanOrEqualTo(4);
        }
    }

    /** Helper: stubs repository calls to return no velocity/dup signals */
    private void stubNoOtherSignals() {
        when(jobOfferRepository.countByClientIdAndCreatedAtAfter(eq(10L), any(LocalDateTime.class)))
                .thenReturn(1L);
        when(jobOfferRepository.countDuplicateTitleSince(eq(10L), nullable(String.class), any(LocalDateTime.class), eq(1L)))
                .thenReturn(0L);
    }
}
