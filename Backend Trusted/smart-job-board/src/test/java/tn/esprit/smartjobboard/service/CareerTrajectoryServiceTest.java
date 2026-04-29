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
import tn.esprit.smartjobboard.entity.CareerSuggestion;
import tn.esprit.smartjobboard.entity.FreelancerProfile;
import tn.esprit.smartjobboard.exception.ForbiddenOperationException;
import tn.esprit.smartjobboard.repository.CareerSuggestionRepository;
import tn.esprit.smartjobboard.repository.FreelancerProfileRepository;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.SkillCooccurrenceRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CareerTrajectoryService")
class CareerTrajectoryServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private MarketAnalyticsService marketAnalyticsService;
    @Mock private SkillCooccurrenceService skillCooccurrenceService;
    @Mock private SkillCooccurrenceRepository skillCooccurrenceRepository;
    @Mock private FreelancerProfileRepository freelancerProfileRepository;
    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private CareerSuggestionRepository careerSuggestionRepository;

    @InjectMocks
    private CareerTrajectoryService service;

    private UserReferenceDto freelancerUser;

    @BeforeEach
    void setUp() {
        freelancerUser = new UserReferenceDto();
        freelancerUser.setId(5L);
        freelancerUser.setRole("FREELANCER");
    }

    @Nested
    @DisplayName("Authorization")
    class Authorization {

        @Test
        @DisplayName("should throw if user is not FREELANCER")
        void notFreelancer() {
            UserReferenceDto clientUser = new UserReferenceDto();
            clientUser.setId(5L);
            clientUser.setRole("CLIENT");
            when(currentUserService.requireCurrentUser()).thenReturn(clientUser);

            assertThatThrownBy(() -> service.insights(5L, null))
                    .isInstanceOf(ForbiddenOperationException.class);
        }

        @Test
        @DisplayName("should throw if user ID doesn't match")
        void wrongUserId() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            assertThatThrownBy(() -> service.insights(999L, null))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Nested
    @DisplayName("insights()")
    class Insights {

        @Test
        @DisplayName("should return default if no current skills")
        void noCurrentSkills() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.empty());

            CareerInsightResponse response = service.insights(5L, null);

            assertThat(response.getTargetRole()).isEqualTo("Developer");
            assertThat(response.getSteps()).isEmpty();
            assertThat(response.getTotalIncomeBoost()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should compute insights and roadmap from market trends")
        void withMarketTrends() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            // Mock profile skills
            FreelancerProfile profile = new FreelancerProfile();
            profile.setSkills(List.of("java"));
            when(freelancerProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));

            // Mock trending market skills
            List<MarketSkillInsightDto> trending = List.of(
                    new MarketSkillInsightDto("docker", 100L, TrendDirection.RISING, 20.0, 80L),
                    new MarketSkillInsightDto("kubernetes", 90L, TrendDirection.RISING, 15.0, 78L)
            );
            when(marketAnalyticsService.topTrendingSkills(30)).thenReturn(trending);
            when(jobOfferRepository.countByStatus(any())).thenReturn(100L);
            when(jobOfferRepository.countJobsWithBothSkills(any(), any())).thenReturn(10L);

            // Mock saveAll
            when(careerSuggestionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            CareerInsightResponse response = service.insights(5L, null);

            assertThat(response.getTargetRole()).contains("java");
            assertThat(response.getSteps()).hasSize(2);
            assertThat(response.getSteps().get(0).getSkillsUnlocked()).contains("docker");

            verify(skillCooccurrenceService).rebuildFromPublishedJobs();
            verify(careerSuggestionRepository).deleteByFreelancerId(5L);
            verify(careerSuggestionRepository).saveAll(any());
        }

        @Test
        @DisplayName("should use fallback skills when market analytics is empty")
        void fallbackSkills() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            List<String> querySkills = List.of("react");
            when(marketAnalyticsService.topTrendingSkills(30)).thenReturn(List.of());
            when(jobOfferRepository.countByStatus(any())).thenReturn(0L); // 0 jobs
            when(careerSuggestionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            CareerInsightResponse response = service.insights(5L, querySkills);

            assertThat(response.getTargetRole()).contains("react");
            assertThat(response.getSteps()).isNotEmpty();
            // Next.js is one of the fallback skills for "react"
            List<String> allUnlocked = response.getSteps().stream()
                    .flatMap(s -> s.getSkillsUnlocked().stream())
                    .toList();
            assertThat(allUnlocked).contains("Next.js");
        }

        @Test
        @DisplayName("should skip skills the user already knows")
        void skipAlreadyKnown() {
            when(currentUserService.requireCurrentUser()).thenReturn(freelancerUser);

            List<String> querySkills = List.of("java", "docker");
            when(marketAnalyticsService.topTrendingSkills(30)).thenReturn(List.of(
                    new MarketSkillInsightDto("docker", 100L, TrendDirection.RISING, 0, 0),
                    new MarketSkillInsightDto("kubernetes", 90L, TrendDirection.RISING, 0, 0)
            ));
            when(jobOfferRepository.countByStatus(any())).thenReturn(100L);
            when(careerSuggestionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            CareerInsightResponse response = service.insights(5L, querySkills);

            // Since docker is known, only kubernetes should remain
            assertThat(response.getSteps()).hasSize(1);
            assertThat(response.getSteps().get(0).getSkillsUnlocked()).contains("kubernetes");
        }
    }
}
