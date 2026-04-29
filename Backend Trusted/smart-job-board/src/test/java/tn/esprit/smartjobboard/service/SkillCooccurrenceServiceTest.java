package tn.esprit.smartjobboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.entity.SkillCooccurrence;
import tn.esprit.smartjobboard.repository.JobOfferRepository;
import tn.esprit.smartjobboard.repository.SkillCooccurrenceRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillCooccurrenceService")
class SkillCooccurrenceServiceTest {

    @Mock private JobOfferRepository jobOfferRepository;
    @Mock private SkillCooccurrenceRepository skillCooccurrenceRepository;
    @Mock private MatchingEngineService matchingEngineService;

    @InjectMocks
    private SkillCooccurrenceService service;

    @Captor
    private ArgumentCaptor<Collection<SkillCooccurrence>> savedCaptor;

    @Test
    @DisplayName("should clear existing records and rebuild from published jobs")
    void rebuildClearsAndPopulates() {
        JobOffer job = new JobOffer();
        job.setId(1L);
        job.setRequiredSkills(List.of("Java", "Docker"));
        job.setExtractedSkills(List.of("Spring Boot"));

        when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of(job));
        when(matchingEngineService.mergeJobSkills(job)).thenReturn(List.of("Java", "Docker", "Spring Boot"));

        service.rebuildFromPublishedJobs();

        verify(skillCooccurrenceRepository).deleteAll();
        verify(skillCooccurrenceRepository).saveAll(savedCaptor.capture());

        Collection<SkillCooccurrence> saved = savedCaptor.getValue();
        // 3 skills → 3 pairs: (docker,java), (docker,spring boot), (java,spring boot)
        assertThat(saved).hasSize(3);
    }

    @Test
    @DisplayName("should produce no pairs for single-skill job")
    void singleSkillNoPairs() {
        JobOffer job = new JobOffer();
        job.setId(1L);
        job.setRequiredSkills(List.of("Java"));
        job.setExtractedSkills(List.of());

        when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of(job));
        when(matchingEngineService.mergeJobSkills(job)).thenReturn(List.of("Java"));

        service.rebuildFromPublishedJobs();

        verify(skillCooccurrenceRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("should merge counts for identical pairs across multiple jobs")
    void mergesCounts() {
        JobOffer job1 = new JobOffer();
        job1.setId(1L);
        JobOffer job2 = new JobOffer();
        job2.setId(2L);

        when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of(job1, job2));
        // Both jobs have {Java, Docker} → pair (docker,java) appears twice
        when(matchingEngineService.mergeJobSkills(job1)).thenReturn(List.of("Java", "Docker"));
        when(matchingEngineService.mergeJobSkills(job2)).thenReturn(List.of("Java", "Docker"));

        service.rebuildFromPublishedJobs();

        verify(skillCooccurrenceRepository).saveAll(savedCaptor.capture());
        Collection<SkillCooccurrence> saved = savedCaptor.getValue();
        // Only 1 merged pair with count=2
        assertThat(saved).hasSize(1);
        assertThat(saved.iterator().next().getCoCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should handle empty published jobs list")
    void noPublishedJobs() {
        when(jobOfferRepository.findByStatus(JobOfferStatus.PUBLISHED)).thenReturn(List.of());

        service.rebuildFromPublishedJobs();

        verify(skillCooccurrenceRepository).deleteAll();
        verify(skillCooccurrenceRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).isEmpty();
    }
}
