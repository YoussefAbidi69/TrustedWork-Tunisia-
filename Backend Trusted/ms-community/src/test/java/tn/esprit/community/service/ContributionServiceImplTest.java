package tn.esprit.community.service;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.entity.Contribution;
import tn.esprit.community.repository.ContributionRepository;
import tn.esprit.community.service.impl.ContributionServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceImplTest {

    @Mock private ContributionRepository contributionRepository;

    @InjectMocks
    private ContributionServiceImpl contributionService;

    @Test
    @DisplayName("shouldCreateContribution_whenNoneExists")
    void shouldCreateContribution_whenNoneExists() {
        when(contributionRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(contributionRepository.save(any(Contribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contribution result = contributionService.recordSharedCourse(5L);

        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("shouldIncrementScore_whenContributionExists")
    void shouldIncrementScore_whenContributionExists() {
        Contribution existing = Contribution.builder().id(1L).userId(7L).score(3).build();
        when(contributionRepository.findByUserId(7L)).thenReturn(Optional.of(existing));
        when(contributionRepository.save(any(Contribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contribution result = contributionService.recordSharedCourse(7L);

        assertThat(result.getScore()).isEqualTo(4);
    }

    @Test
    @DisplayName("shouldReturnNull_whenContributionMissing")
    void shouldReturnNull_whenContributionMissing() {
        when(contributionRepository.findByUserId(12L)).thenReturn(Optional.empty());

        Contribution result = contributionService.getContribution(12L);

        assertThat(result).isNull();
    }
}
