package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.Section;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.impl.SectionServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceImplTest {

    @Mock private SectionRepository sectionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private SectionServiceImpl sectionService;

    @Test
    @DisplayName("shouldCreateSectionWithNextOrder_whenOrderIndexNotProvided")
    void shouldCreateSectionWithNextOrder_whenOrderIndexNotProvided() {
        Course course = Course.builder().id(1L).build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Section existing = Section.builder().orderIndex(2).build();
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(existing));

        Section saved = Section.builder().id(10L).course(course).title("Intro").orderIndex(3).build();
        when(sectionRepository.save(any(Section.class))).thenReturn(saved);
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());

        SectionRequest request = SectionRequest.builder().title("Intro").build();
        SectionResponse response = sectionService.createSection(1L, request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getOrderIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseNotFoundOnCreate")
    void shouldThrowLearningNotFoundException_whenCourseNotFoundOnCreate() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.createSection(99L, SectionRequest.builder().build()))
                .isInstanceOf(LearningNotFoundException.class);
    }

    @Test
    @DisplayName("shouldUpdateSectionFields_whenProvidedInRequest")
    void shouldUpdateSectionFields_whenProvidedInRequest() {
        Section existing = Section.builder().id(2L).title("Old").orderIndex(1).build();
        when(sectionRepository.findById(2L)).thenReturn(Optional.of(existing));

        Section updated = Section.builder().id(2L).title("New").orderIndex(5).build();
        when(sectionRepository.save(existing)).thenReturn(updated);
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(2L)).thenReturn(List.of());

        SectionRequest request = SectionRequest.builder().title("New").orderIndex(5).build();
        SectionResponse response = sectionService.updateSection(2L, request);

        assertThat(response.getTitle()).isEqualTo("New");
        assertThat(response.getOrderIndex()).isEqualTo(5);
    }

    @Test
    @DisplayName("shouldListSectionsByCourse")
    void shouldListSectionsByCourse() {
        Section s = Section.builder().id(1L).title("A").build();
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(3L)).thenReturn(List.of(s));
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        List<SectionResponse> responses = sectionService.listSections(3L);

        assertThat(responses).hasSize(1);
    }

    @Test
    @DisplayName("shouldGetSectionById")
    void shouldGetSectionById() {
        Section s = Section.builder().id(1L).title("A").build();
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(blockRepository.findBySectionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        SectionResponse response = sectionService.getSection(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldDeleteSection")
    void shouldDeleteSection() {
        when(sectionRepository.existsById(5L)).thenReturn(true);
        sectionService.deleteSection(5L);
        verify(sectionRepository).deleteById(5L);
    }
}
