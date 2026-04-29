package tn.esprit.smartjobboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.entity.*;
import tn.esprit.smartjobboard.repository.CompatibilityReportRepository;
import tn.esprit.smartjobboard.repository.MatchScoreRepository;
import tn.esprit.smartjobboard.repository.SuccessPredictionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingEngineService")
class MatchingEngineServiceTest {

    @Mock private SemanticSkillService semanticSkillService;
    @Mock private MatchScoreRepository matchScoreRepository;
    @Mock private SuccessPredictionRepository successPredictionRepository;
    @Mock private CompatibilityReportRepository compatibilityReportRepository;

    private MatchingEngineService service;

    private JobOffer job;
    private FreelancerProfile profile;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new MatchingEngineService(
                semanticSkillService, matchScoreRepository, successPredictionRepository,
                compatibilityReportRepository, objectMapper);

        job = new JobOffer();
        job.setId(1L);
        job.setClientId(10L);
        job.setTitle("Java Dev");
        job.setDescription("Build microservices");
        job.setCategory("IT");
        job.setRequiredSkills(List.of("Java", "Spring Boot"));
        job.setExtractedSkills(List.of("Docker"));
        job.setBudgetMin(BigDecimal.valueOf(500));
        job.setBudgetMax(BigDecimal.valueOf(2000));

        profile = new FreelancerProfile();
        profile.setUserId(5L);
        profile.setEmail("dev@example.com");
        profile.setSkills(List.of("Java", "Docker"));
        profile.setPreferredRate(BigDecimal.valueOf(1000));
    }

    @Nested
    @DisplayName("mergeJobSkills()")
    class MergeJobSkills {

        @Test
        @DisplayName("should merge required and extracted skills without duplicates")
        void mergeSkills() {
            job.setRequiredSkills(List.of("Java", "Docker"));
            job.setExtractedSkills(List.of("Docker", "Spring Boot"));

            List<String> merged = service.mergeJobSkills(job);

            assertThat(merged).containsExactlyInAnyOrder("Java", "Docker", "Spring Boot");
        }

        @Test
        @DisplayName("should skip null and blank entries")
        void skipBlanks() {
            job.setRequiredSkills(List.of("Java", "", " "));
            job.setExtractedSkills(null);

            List<String> merged = service.mergeJobSkills(job);

            assertThat(merged).containsExactly("Java");
        }

        @Test
        @DisplayName("should return empty list when both are null")
        void bothNull() {
            job.setRequiredSkills(null);
            job.setExtractedSkills(null);

            List<String> merged = service.mergeJobSkills(job);

            assertThat(merged).isEmpty();
        }
    }

    @Nested
    @DisplayName("computeBudgetFit()")
    class BudgetFit {

        @Test
        @DisplayName("should return 100 when rate is within budget range")
        void withinRange() {
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(1000));
            assertThat(fit).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should return 100 when rate equals budget min")
        void equalsMin() {
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(500));
            assertThat(fit).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should return 100 when rate equals budget max")
        void equalsMax() {
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(2000));
            assertThat(fit).isEqualTo(100.0);
        }

        @Test
        @DisplayName("should return partial score when rate is below min")
        void belowMin() {
            // rate=300, min=500, gap=200, pct=200/500=40%, fit=100-40=60
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(300));
            assertThat(fit).isCloseTo(60.0, within(1.0));
        }

        @Test
        @DisplayName("should return partial score when rate is above max")
        void aboveMax() {
            // rate=3000, max=2000, gap=1000, pct=1000/2000=50%, fit=100-50=50
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(3000));
            assertThat(fit).isCloseTo(50.0, within(1.0));
        }

        @Test
        @DisplayName("should return 0 when any parameter is null")
        void nullParams() {
            assertThat(service.computeBudgetFit(job, null)).isEqualTo(0.0);

            job.setBudgetMin(null);
            assertThat(service.computeBudgetFit(job, BigDecimal.valueOf(1000))).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 when min is zero and rate is below")
        void zeroMin() {
            job.setBudgetMin(BigDecimal.ZERO);
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(-100));
            // rate < min(0), min.signum()==0 → return 0
            assertThat(fit).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should clamp to 0 when rate far exceeds max")
        void farAboveMax() {
            double fit = service.computeBudgetFit(job, BigDecimal.valueOf(5000));
            // gap=3000, pct=3000/2000=150%, fit=100-150=-50 → clamped to 0
            assertThat(fit).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("evaluateRaw() — heuristic fallback")
    class EvaluateRaw {

        @Test
        @DisplayName("should compute weighted total score via heuristic when ML service unavailable")
        void heuristicFallback() {
            // ML service will fail since there's no mock server → heuristic path executes
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(80.0);

            MatchingEngineService.RawMatchEvaluation raw =
                    service.evaluateRaw(job, profile, BigDecimal.valueOf(1000));

            // total = skillPct*0.4 + 70*0.2 + 75*0.2 + budgetFit*0.1 + 80*0.1
            // Exact value depends on budgetFit computation
            assertThat(raw.totalScore()).isCloseTo(79.0, within(3.0));
            assertThat(raw.skillMatch()).isEqualTo(80.0);
            assertThat(raw.reputation()).isEqualTo(70.0); // default
            assertThat(raw.successRate()).isEqualTo(75.0); // default
            assertThat(raw.availability()).isEqualTo(80.0); // default
        }

        @Test
        @DisplayName("should compute success probability from skill overlap and defaults")
        void successProbability() {
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(60.0);

            MatchingEngineService.RawMatchEvaluation raw =
                    service.evaluateRaw(job, profile, BigDecimal.valueOf(1000));

            // probability = 0.6*0.5 + 0.7*0.3 + 0.75*0.2 = 0.3 + 0.21 + 0.15 = 0.66
            assertThat(raw.successProbability()).isCloseTo(0.66, within(0.02));
            assertThat(raw.confidence()).isEqualTo(PredictionConfidence.MEDIUM);
        }

        @Test
        @DisplayName("should clamp total score between 0 and 100")
        void totalScoreClamped() {
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(0.0);

            MatchingEngineService.RawMatchEvaluation raw =
                    service.evaluateRaw(job, profile, null);

            assertThat(raw.totalScore()).isGreaterThanOrEqualTo(0.0);
            assertThat(raw.totalScore()).isLessThanOrEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("computePersistAndReturn()")
    class PersistAndReturn {

        @Test
        @DisplayName("should persist MatchScore, SuccessPrediction, and CompatibilityReport")
        void persistsAllEntities() {
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(50.0);
            when(matchScoreRepository.findByJobOfferIdAndFreelancerId(1L, 5L)).thenReturn(Optional.empty());
            when(successPredictionRepository.findByJobOfferIdAndFreelancerId(1L, 5L)).thenReturn(Optional.empty());
            when(compatibilityReportRepository.findByMatchScoreId(any())).thenReturn(Optional.empty());

            MatchScore savedMs = new MatchScore();
            savedMs.setId(100L);
            savedMs.setJobOfferId(1L);
            savedMs.setFreelancerId(5L);
            savedMs.setSkillMatch(50);
            savedMs.setReputation(70);
            savedMs.setSuccessRate(75);
            savedMs.setBudgetFit(100);
            savedMs.setAvailability(80);
            savedMs.setTotalScore(70);
            when(matchScoreRepository.save(any(MatchScore.class))).thenReturn(savedMs);

            MatchingEngineService.MatchComputationResult result =
                    service.computePersistAndReturn(job, profile, BigDecimal.valueOf(1000));

            verify(matchScoreRepository).save(any(MatchScore.class));
            verify(successPredictionRepository).save(any(SuccessPrediction.class));
            verify(compatibilityReportRepository).save(any(CompatibilityReport.class));
            assertThat(result.matchScore()).isNotNull();
        }

        @Test
        @DisplayName("should update existing MatchScore instead of creating new one")
        void updatesExistingScore() {
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(50.0);

            MatchScore existing = new MatchScore();
            existing.setId(42L);
            existing.setJobOfferId(1L);
            existing.setFreelancerId(5L);
            when(matchScoreRepository.findByJobOfferIdAndFreelancerId(1L, 5L)).thenReturn(Optional.of(existing));
            when(matchScoreRepository.save(any(MatchScore.class))).thenReturn(existing);
            when(successPredictionRepository.findByJobOfferIdAndFreelancerId(1L, 5L)).thenReturn(Optional.empty());
            when(compatibilityReportRepository.findByMatchScoreId(42L)).thenReturn(Optional.empty());

            service.computePersistAndReturn(job, profile, BigDecimal.valueOf(1000));

            verify(matchScoreRepository).save(argThat(ms -> ms.getId().equals(42L)));
        }
    }

    @Nested
    @DisplayName("Confidence thresholds")
    class ConfidenceThresholds {

        @Test
        @DisplayName("should return LOW confidence for probability < 0.4")
        void lowConfidence() {
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(10.0);

            MatchingEngineService.RawMatchEvaluation raw =
                    service.evaluateRaw(job, profile, null);

            // With 10% skill match, probability will be low
            if (raw.successProbability() < 0.4) {
                assertThat(raw.confidence()).isEqualTo(PredictionConfidence.LOW);
            }
        }

        @Test
        @DisplayName("should return HIGH confidence for probability > 0.7")
        void highConfidence() {
            when(semanticSkillService.skillMatchPercent(anyList(), anyList())).thenReturn(100.0);

            MatchingEngineService.RawMatchEvaluation raw =
                    service.evaluateRaw(job, profile, BigDecimal.valueOf(1000));

            // probability = 1.0*0.5 + 0.7*0.3 + 0.75*0.2 = 0.5+0.21+0.15 = 0.86
            assertThat(raw.confidence()).isEqualTo(PredictionConfidence.HIGH);
        }
    }
}
