package tn.esprit.smartjobboard.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tn.esprit.smartjobboard.dto.*;
import tn.esprit.smartjobboard.entity.*;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.exception.JobNotEditableException;
import tn.esprit.smartjobboard.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobOfferService")
class JobOfferServiceTest {

    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private OfferFlagRepository offerFlagRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private MatchScoreRepository matchScoreRepository;
    @Mock private CompatibilityReportRepository compatibilityReportRepository;
    @Mock private SuccessPredictionRepository successPredictionRepository;
    @Mock private BudgetIntelligenceRepository budgetIntelligenceRepository;
    @Mock private JobDemandSnapshotRepository jobDemandSnapshotRepository;
    @Mock private OpportunityNotificationLogRepository opportunityNotificationLogRepository;
    @Mock private FreelancerProfileRepository freelancerProfileRepository;
    @Mock private SkillExtractionService skillExtractionService;
    @Mock private FraudDetectionService fraudDetectionService;
    @Mock private OpportunityScoreService opportunityScoreService;
    @Mock private MatchingEngineService matchingEngineService;
    @Mock private CurrentUserService currentUserService;

    @InjectMocks
    private JobOfferService service;

    private UserReferenceDto clientUser;
    private UserReferenceDto freelancerUser;

    @BeforeEach
    void setUp() {
        clientUser = new UserReferenceDto();
        clientUser.setId(10L);
        clientUser.setRole("CLIENT");

        freelancerUser = new UserReferenceDto();
        freelancerUser.setId(5L);
        freelancerUser.setRole("FREELANCER");
    }

    @Nested
    @DisplayName("previewSkills()")
    class PreviewSkills {
        @Test
        @DisplayName("should extract skills from description")
        void success() {
            PreviewSkillsRequest req = new PreviewSkillsRequest();
            req.setDescription("Need Java and React");

            when(skillExtractionService.extractFromDescription("Need Java and React"))
                    .thenReturn(List.of("Java", "React"));

            PreviewSkillsResponse res = service.previewSkills(req);
            assertThat(res.getSkills()).containsExactly("Java", "React");
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {
        @Test
        @DisplayName("should throw if not CLIENT")
        void notClient() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            assertThatThrownBy(() -> service.create(new JobOfferCreateRequest()))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("should create draft job")
        void success() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);

            JobOfferCreateRequest req = new JobOfferCreateRequest();
            req.setTitle("Java Dev");
            req.setDescription("Desc");
            req.setCategory("IT");
            req.setBudgetMin(BigDecimal.valueOf(100));
            req.setBudgetMax(BigDecimal.valueOf(200));
            req.setRequiredSkills(List.of("Java"));

            when(skillExtractionService.extractFromDescription("Desc")).thenReturn(List.of("Java"));

            JobOffer mockSaved = new JobOffer();
            mockSaved.setId(1L);
            mockSaved.setTitle("Java Dev");
            mockSaved.setClientId(10L);
            mockSaved.setStatus(JobOfferStatus.DRAFT);
            when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(mockSaved);

            // Mock fraud check
            FraudDetectionService.FraudAssessment fraud = new FraudDetectionService.FraudAssessment(0.0, List.of());
            when(fraudDetectionService.assess(any(), any())).thenReturn(fraud);
            doNothing().when(opportunityScoreService).computeAndPersist(any());

            JobOfferResponse res = service.create(req);

            assertThat(res.getId()).isEqualTo(1L);
            assertThat(res.getStatus()).isEqualTo(JobOfferStatus.DRAFT);
            verify(jobOfferRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {
        @Test
        @DisplayName("should throw if not owner")
        void notOwner() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobOffer job = new JobOffer();
            job.setClientId(99L);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> service.update(1L, new JobOfferUpdateRequest()))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("should throw if published")
        void published() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobOffer job = new JobOffer();
            job.setClientId(10L);
            job.setStatus(JobOfferStatus.PUBLISHED);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> service.update(1L, new JobOfferUpdateRequest()))
                    .isInstanceOf(JobNotEditableException.class);
        }

        @Test
        @DisplayName("should update draft job")
        void success() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setClientId(10L);
            job.setStatus(JobOfferStatus.DRAFT);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            JobOfferUpdateRequest req = new JobOfferUpdateRequest();
            req.setTitle("Updated");
            req.setDescription("Desc");
            req.setCategory("IT");
            req.setBudgetMin(BigDecimal.ZERO);
            req.setBudgetMax(BigDecimal.TEN);

            when(jobOfferRepository.save(any(JobOffer.class))).thenReturn(job);
            FraudDetectionService.FraudAssessment fraud = new FraudDetectionService.FraudAssessment(0.0, List.of());
            when(fraudDetectionService.assess(any(), any())).thenReturn(fraud);
            doNothing().when(opportunityScoreService).computeAndPersist(any());

            JobOfferResponse res = service.update(1L, req);

            assertThat(job.getTitle()).isEqualTo("Updated");
            verify(jobOfferRepository, times(2)).save(job);
        }
    }

    @Nested
    @DisplayName("get()")
    class Get {
        @Test
        @DisplayName("should return if published")
        void published() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setClientId(10L);
            job.setStatus(JobOfferStatus.PUBLISHED);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            JobOfferResponse res = service.get(1L);
            assertThat(res.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw if draft and not owner")
        void draftNotOwner() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setClientId(10L);
            job.setStatus(JobOfferStatus.DRAFT);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> service.get(1L))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Nested
    @DisplayName("getSuccessPrediction()")
    class SuccessPrediction {
        @Test
        @DisplayName("should compute success prediction")
        void success() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setClientId(10L);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));

            tn.esprit.smartjobboard.entity.SuccessPrediction sp = new tn.esprit.smartjobboard.entity.SuccessPrediction();
            sp.setProbability(0.85);
            sp.setConfidence(PredictionConfidence.HIGH);
            when(successPredictionRepository.findByJobOfferIdAndFreelancerId(1L, 5L))
                    .thenReturn(Optional.of(sp));

            when(matchingEngineService.mergeJobSkills(any())).thenReturn(List.of("Java"));
            FreelancerProfile fp = new FreelancerProfile();
            fp.setSkills(List.of("Java"));
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(fp));

            SuccessPredictionViewDto res = service.getSuccessPrediction(1L, 5L);

            assertThat(res.getProbability()).isEqualTo(0.85);
            assertThat(res.getConfidenceLabel()).isEqualTo("HIGH");
            assertThat(res.getSkillOverlapPercent()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("publish()")
    class Publish {
        @Test
        @DisplayName("should publish draft and log intelligence")
        void success() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);

            JobOffer job = new JobOffer();
            job.setId(1L);
            job.setClientId(10L);
            job.setStatus(JobOfferStatus.DRAFT);
            job.setRequiredSkills(List.of("Java"));
            job.setBudgetMin(BigDecimal.valueOf(100));
            job.setBudgetMax(BigDecimal.valueOf(200));

            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(job));
            when(jobOfferRepository.save(any())).thenReturn(job);
            FraudDetectionService.FraudAssessment fraud = new FraudDetectionService.FraudAssessment(0.0, List.of());
            when(fraudDetectionService.assess(any(), any())).thenReturn(fraud);
            doNothing().when(opportunityScoreService).computeAndPersist(any());

            JobOfferResponse res = service.publish(1L);

            assertThat(res.getStatus()).isEqualTo(JobOfferStatus.PUBLISHED);
        }
    }
}


