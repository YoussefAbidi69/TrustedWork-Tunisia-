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
import tn.esprit.smartjobboard.dto.*;
import tn.esprit.smartjobboard.entity.*;
import tn.esprit.smartjobboard.exception.DuplicateApplicationException;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.exception.InvalidStatusTransitionException;
import tn.esprit.smartjobboard.exception.JobClosedException;
import tn.esprit.smartjobboard.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobApplicationService")
class JobApplicationServiceTest {

    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private FreelancerProfileRepository freelancerProfileRepository;
    @Mock private OpportunityScoreService opportunityScoreService;
    @Mock private CurrentUserService currentUserService;
    @Mock private MatchingEngineService matchingEngineService;
    @Mock private MatchScoreRepository matchScoreRepository;
    @Mock private SuccessPredictionRepository successPredictionRepository;
    @Mock private FreelancerProfileClient freelancerProfileClient;

    @InjectMocks
    private JobApplicationService service;

    private UserReferenceDto freelancerUser;
    private UserReferenceDto clientUser;
    private JobOffer publishedJob;
    private ApplicationCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        freelancerUser = new UserReferenceDto();
        freelancerUser.setId(5L);
        freelancerUser.setEmail("dev@example.com");
        freelancerUser.setRole("FREELANCER");
        freelancerUser.setFirstName("John");
        freelancerUser.setLastName("Doe");

        clientUser = new UserReferenceDto();
        clientUser.setId(10L);
        clientUser.setEmail("client@example.com");
        clientUser.setRole("CLIENT");

        publishedJob = new JobOffer();
        publishedJob.setId(1L);
        publishedJob.setClientId(10L);
        publishedJob.setTitle("Java Developer");
        publishedJob.setDescription("Build microservices");
        publishedJob.setCategory("IT");
        publishedJob.setLocation("Remote");
        publishedJob.setStatus(JobOfferStatus.PUBLISHED);
        publishedJob.setBudgetMin(BigDecimal.valueOf(500));
        publishedJob.setBudgetMax(BigDecimal.valueOf(2000));
        publishedJob.setRequiredSkills(List.of("Java"));

        createRequest = new ApplicationCreateRequest();
        createRequest.setJobOfferId(1L);
        createRequest.setCoverLetter("I am a skilled Java developer with 5 years of experience.");
        createRequest.setProposedRate(BigDecimal.valueOf(1000));
        createRequest.setDeclaredSkills(List.of("Java", "Docker"));
    }

    // ─────────────────────── submit() ───────────────────────

    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("should successfully submit application for a FREELANCER to a PUBLISHED job")
        void happyPath() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));
            when(jobApplicationRepository.existsByJobOfferIdAndFreelancerId(1L, 5L)).thenReturn(false);

            FreelancerProfile fp = new FreelancerProfile();
            fp.setUserId(5L);
            fp.setEmail("dev@example.com");
            fp.setSkills(List.of("Java", "Docker"));
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(fp));
            when(freelancerProfileRepository.save(any(FreelancerProfile.class))).thenReturn(fp);

            JobApplication saved = buildApp(1L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(saved);

            MatchScore ms = new MatchScore();
            ms.setId(1L); ms.setSkillMatch(80); ms.setReputation(70); ms.setSuccessRate(75);
            ms.setBudgetFit(100); ms.setAvailability(80); ms.setTotalScore(79);
            SuccessPrediction sp = new SuccessPrediction();
            sp.setProbability(0.75); sp.setConfidence(PredictionConfidence.HIGH);
            MatchingEngineService.MatchComputationResult comp =
                    new MatchingEngineService.MatchComputationResult(ms, sp, new CompatibilityReport());
            when(matchingEngineService.computePersistAndReturn(any(), any(), any())).thenReturn(comp);
            when(jobOfferRepository.save(any())).thenReturn(publishedJob);

            JobApplicationResponse response = service.submit(createRequest);

            assertThat(response.getJobOfferId()).isEqualTo(1L);
            assertThat(response.getFreelancerId()).isEqualTo(5L);
            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.PENDING);
            verify(jobApplicationRepository).save(any(JobApplication.class));
            verify(opportunityScoreService).computeAndPersist(publishedJob);
        }

        @Test
        @DisplayName("should throw ForbiddenOperationException when user is not FREELANCER")
        void wrongRole() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);

            assertThatThrownBy(() -> service.submit(createRequest))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("FREELANCER");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when job does not exist")
        void jobNotFound() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submit(createRequest))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should throw JobClosedException when job is CLOSED")
        void closedJob() {
            publishedJob.setStatus(JobOfferStatus.CLOSED);
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));

            assertThatThrownBy(() -> service.submit(createRequest))
                    .isInstanceOf(JobClosedException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        @DisplayName("should throw JobClosedException when job is FLAGGED")
        void flaggedJob() {
            publishedJob.setStatus(JobOfferStatus.FLAGGED);
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));

            assertThatThrownBy(() -> service.submit(createRequest))
                    .isInstanceOf(JobClosedException.class)
                    .hasMessageContaining("review");
        }

        @Test
        @DisplayName("should throw JobClosedException when job is DRAFT")
        void draftJob() {
            publishedJob.setStatus(JobOfferStatus.DRAFT);
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));

            assertThatThrownBy(() -> service.submit(createRequest))
                    .isInstanceOf(JobClosedException.class)
                    .hasMessageContaining("not open");
        }

        @Test
        @DisplayName("should throw DuplicateApplicationException when already applied")
        void duplicateApplication() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));
            when(jobApplicationRepository.existsByJobOfferIdAndFreelancerId(1L, 5L)).thenReturn(true);

            assertThatThrownBy(() -> service.submit(createRequest))
                    .isInstanceOf(DuplicateApplicationException.class)
                    .hasMessageContaining("already applied");
        }
    }

    // ─────────────────────── listMineForFreelancer() ───────────────────────

    @Nested
    @DisplayName("listMineForFreelancer()")
    class ListMine {

        @Test
        @DisplayName("should return applications sorted by appliedAt descending")
        void sortedByDate() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            JobApplication app1 = buildApp(1L, publishedJob, 5L, ApplicationStatus.PENDING);
            app1.setAppliedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
            JobApplication app2 = buildApp(2L, publishedJob, 5L, ApplicationStatus.ACCEPTED);
            app2.setAppliedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
            when(jobApplicationRepository.findMineWithJobOffer(5L)).thenReturn(List.of(app1, app2));

            List<JobApplicationResponse> result = service.listMineForFreelancer();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAppliedAt()).isAfter(result.get(1).getAppliedAt());
        }

        @Test
        @DisplayName("should throw when user is not FREELANCER")
        void wrongRole() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);

            assertThatThrownBy(() -> service.listMineForFreelancer())
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    // ─────────────────────── listForJob() ───────────────────────

    @Nested
    @DisplayName("listForJob()")
    class ListForJob {

        @Test
        @DisplayName("should return applications for a client's own job")
        void ownJob() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));
            when(jobApplicationRepository.findByJobOfferIdWithJob(1L)).thenReturn(List.of());

            List<JobApplicationResponse> result = service.listForJob(1L);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ForbiddenOperationException when client does not own the job")
        void notOwner() {
            clientUser.setId(999L); // different from job.clientId
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(publishedJob));

            assertThatThrownBy(() -> service.listForJob(1L))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("own jobs");
        }

        @Test
        @DisplayName("should throw when user is not CLIENT")
        void wrongRole() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            assertThatThrownBy(() -> service.listForJob(1L))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    // ─────────────────────── updateStatus() ───────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("should transition PENDING → SHORTLISTED")
        void pendingToShortlisted() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
            when(jobApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.SHORTLISTED);
            JobApplicationResponse response = service.updateStatus(100L, req);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        }

        @Test
        @DisplayName("should transition PENDING → REJECTED")
        void pendingToRejected() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
            when(jobApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.REJECTED);
            JobApplicationResponse response = service.updateStatus(100L, req);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("should transition PENDING → ACCEPTED")
        void pendingToAccepted() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
            when(jobApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.ACCEPTED);
            JobApplicationResponse response = service.updateStatus(100L, req);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should transition SHORTLISTED → ACCEPTED")
        void shortlistedToAccepted() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.SHORTLISTED);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
            when(jobApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.ACCEPTED);
            JobApplicationResponse response = service.updateStatus(100L, req);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should throw InvalidStatusTransitionException for PENDING → WITHDRAWN via client")
        void invalidTransitionPendingToWithdrawn() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.WITHDRAWN);

            assertThatThrownBy(() -> service.updateStatus(100L, req))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("should throw for ACCEPTED → anything (terminal)")
        void acceptedTerminal() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.ACCEPTED);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.REJECTED);

            assertThatThrownBy(() -> service.updateStatus(100L, req))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Accepted");
        }

        @Test
        @DisplayName("should throw for WITHDRAWN → anything (terminal)")
        void withdrawnTerminal() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.WITHDRAWN);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.ACCEPTED);

            assertThatThrownBy(() -> service.updateStatus(100L, req))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Terminal");
        }

        @Test
        @DisplayName("should throw for REJECTED → anything (terminal)")
        void rejectedTerminal() {
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.REJECTED);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.PENDING);

            assertThatThrownBy(() -> service.updateStatus(100L, req))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Terminal");
        }

        @Test
        @DisplayName("should throw ForbiddenOperationException when client doesn't own the job")
        void notOwner() {
            clientUser.setId(999L);
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
            req.setStatus(ApplicationStatus.SHORTLISTED);

            assertThatThrownBy(() -> service.updateStatus(100L, req))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    // ─────────────────────── withdraw() ───────────────────────

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        @DisplayName("should withdraw own PENDING application")
        void happyPath() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));
            when(jobApplicationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(jobOfferRepository.save(any())).thenReturn(publishedJob);

            JobApplicationResponse response = service.withdraw(100L);

            assertThat(response.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
            verify(opportunityScoreService).computeAndPersist(publishedJob);
        }

        @Test
        @DisplayName("should throw when withdrawing someone else's application")
        void notOwner() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            JobApplication app = buildApp(100L, publishedJob, 999L, ApplicationStatus.PENDING);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.withdraw(100L))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("own applications");
        }

        @Test
        @DisplayName("should throw when application is not PENDING")
        void notPending() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            JobApplication app = buildApp(100L, publishedJob, 5L, ApplicationStatus.SHORTLISTED);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(app));

            assertThatThrownBy(() -> service.withdraw(100L))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("pending");
        }

        @Test
        @DisplayName("should throw when application not found")
        void notFound() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(jobApplicationRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.withdraw(100L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ─────────────────────── helpers ───────────────────────

    private JobApplication buildApp(Long id, JobOffer job, Long freelancerId, ApplicationStatus status) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setJobOffer(job);
        app.setFreelancerId(freelancerId);
        app.setCoverLetter("Test cover letter with sufficient length for validation.");
        app.setProposedRate(BigDecimal.valueOf(1000));
        app.setStatus(status);
        app.setAppliedAt(LocalDateTime.now());
        return app;
    }
}
