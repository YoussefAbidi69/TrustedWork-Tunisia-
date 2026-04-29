package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.dto.JobOfferResponse;
import tn.esprit.smartjobboard.dto.JobRecommendationRowDto;
import tn.esprit.smartjobboard.dto.UserReferenceDto;
import tn.esprit.smartjobboard.entity.*;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService")
class RecommendationServiceTest {

    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private FreelancerProfileRepository freelancerProfileRepository;
    @Mock private MatchingEngineService matchingEngineService;
    @Mock private CurrentUserService currentUserService;
    @Mock private JobOfferService jobOfferService;

    @InjectMocks
    private RecommendationService service;

    private UserReferenceDto freelancerUser;
    private FreelancerProfile profile;

    @BeforeEach
    void setUp() {
        freelancerUser = new UserReferenceDto();
        freelancerUser.setId(5L);
        freelancerUser.setEmail("dev@example.com");
        freelancerUser.setRole("FREELANCER");

        profile = new FreelancerProfile();
        profile.setUserId(5L);
        profile.setEmail("dev@example.com");
        profile.setSkills(List.of("Java", "Docker"));
        profile.setPreferredRate(BigDecimal.valueOf(1000));
    }

    @Nested
    @DisplayName("Authorization")
    class Authorization {

        @Test
        @DisplayName("should throw when user is not FREELANCER")
        void notFreelancer() {
            UserReferenceDto client = new UserReferenceDto();
            client.setId(10L);
            client.setRole("CLIENT");
            when(currentUserService.requireCurrentUser()).thenReturn(client);

            assertThatThrownBy(() -> service.recommend(10L, null))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("FREELANCER");
        }

        @Test
        @DisplayName("should throw when requesting for another user")
        void wrongUser() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            assertThatThrownBy(() -> service.recommend(999L, null))
                    .isInstanceOf(ForbiddenOperationException.class)
                    .hasMessageContaining("own account");
        }
    }

    @Nested
    @DisplayName("recommend()")
    class Recommend {

        @Test
        @DisplayName("should return recommendations for freelancer")
        void happyPath() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));

            JobOffer job = buildPublishedJob(1L, "Java Developer");
            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of(job));

            MatchingEngineService.RawMatchEvaluation raw =
                    new MatchingEngineService.RawMatchEvaluation(80, 70, 75, 100, 80, 79, 0.75, PredictionConfidence.HIGH);
            when(matchingEngineService.evaluateRaw(any(), any(), any())).thenReturn(raw);
            when(matchingEngineService.mergeJobSkills(any())).thenReturn(List.of("Java", "Docker"));

            JobOfferResponse mockResp = JobOfferResponse.builder().id(1L).title("Java Developer").build();
            when(jobOfferService.asResponse(any())).thenReturn(mockResp);

            List<JobRecommendationRowDto> result = service.recommend(5L, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Java Developer");
            assertThat(result.get(0).getMatchScore()).isEqualTo(79.0);
            verify(matchingEngineService).persistRaw(eq(job), eq(profile), eq(raw));
        }

        @Test
        @DisplayName("should limit to top 10 results")
        void limitsTo10() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));

            List<JobOffer> jobs = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                jobs.add(buildPublishedJob((long) (i + 1), "Job " + i));
            }
            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(jobs);

            MatchingEngineService.RawMatchEvaluation raw =
                    new MatchingEngineService.RawMatchEvaluation(50, 60, 70, 80, 75, 65, 0.5, PredictionConfidence.MEDIUM);
            when(matchingEngineService.evaluateRaw(any(), any(), any())).thenReturn(raw);
            when(matchingEngineService.mergeJobSkills(any())).thenReturn(List.of());

            JobOfferResponse mockResp = JobOfferResponse.builder().id(1L).build();
            when(jobOfferService.asResponse(any())).thenReturn(mockResp);

            List<JobRecommendationRowDto> result = service.recommend(5L, null);

            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should use override skills when provided")
        void overrideSkills() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));
            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of());

            List<JobRecommendationRowDto> result = service.recommend(5L, List.of("Python", "React"));

            assertThat(result).isEmpty(); // no jobs, but should not throw
        }

        @Test
        @DisplayName("should create new profile when freelancer has none")
        void noExistingProfile() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.empty());
            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of());

            List<JobRecommendationRowDto> result = service.recommend(5L, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no published jobs")
        void noPublishedJobs() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));
            when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of());

            List<JobRecommendationRowDto> result = service.recommend(5L, null);

            assertThat(result).isEmpty();
        }
    }

    private JobOffer buildPublishedJob(Long id, String title) {
        JobOffer job = new JobOffer();
        job.setId(id);
        job.setClientId(10L);
        job.setTitle(title);
        job.setDescription("Test description");
        job.setCategory("IT");
        job.setRequiredSkills(new ArrayList<>(List.of("Java")));
        job.setExtractedSkills(new ArrayList<>());
        job.setBudgetMin(BigDecimal.valueOf(500));
        job.setBudgetMax(BigDecimal.valueOf(2000));
        job.setStatus(JobOfferStatus.PUBLISHED);
        job.setPublishedAt(LocalDateTime.now().minusDays(2));
        job.setOpportunityScore(60.0);
        return job;
    }
}
