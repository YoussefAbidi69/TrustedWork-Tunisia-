package tn.esprit.community.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.entity.Block;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.Section;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.repository.BlockRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.impl.SectionServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceImplTest {

    @Mock private SectionRepository sectionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private BlockRepository blockRepository;

    @InjectMocks
    private SectionServiceImpl sectionService;

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseMissing")
    void shouldThrowLearningNotFoundException_whenCourseMissing() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.createSection(9L, SectionRequest.builder().title("T").build()))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    @DisplayName("shouldDefaultOrderIndex_whenMissingInRequest")
    void shouldDefaultOrderIndex_whenMissingInRequest() {
        Course course = Course.builder().id(1L).build();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(sectionRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(
                Section.builder().orderIndex(0).build(),
                Section.builder().orderIndex(3).build()
        ));
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SectionResponse response = sectionService.createSection(1L, SectionRequest.builder().title("Intro").build());

        ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
        verify(sectionRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderIndex()).isEqualTo(4);
        assertThat(response.getOrderIndex()).isEqualTo(4);
    }

    @Test
    @DisplayName("shouldUpdateFields_whenRequestProvidesValues")
    void shouldUpdateFields_whenRequestProvidesValues() {
        Section existing = Section.builder().id(3L).title("Old").orderIndex(1).build();
        when(sectionRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(sectionRepository.save(existing)).thenReturn(existing);

        SectionResponse response = sectionService.updateSection(3L, SectionRequest.builder()
                .title("New")
                .orderIndex(2)
                .build());

        assertThat(response.getTitle()).isEqualTo("New");
        assertThat(response.getOrderIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("shouldMapBlocks_whenListingSections")
    void shouldMapBlocks_whenListingSections() {
        Course course = Course.builder().id(1L).build();
        Section section = Section.builder().id(10L).course(course).title("S").orderIndex(0).build();
        when(sectionRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(section));
        when(blockRepository.findBySection_IdOrderByOrderIndexAsc(10L)).thenReturn(List.of(
                Block.builder().id(1L).title("B").orderIndex(0).build()
        ));

        List<SectionResponse> responses = sectionService.listSections(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBlocks()).hasSize(1);
        assertThat(responses.get(0).getBlocks().get(0).getTitle()).isEqualTo("B");
    }

    @Test
    @DisplayName("shouldDeleteSection_whenExists")
    void shouldDeleteSection_whenExists() {
        when(sectionRepository.existsById(5L)).thenReturn(true);

        sectionService.deleteSection(5L);

        verify(sectionRepository).deleteById(5L);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenDeleteMissing")
    void shouldThrowLearningNotFoundException_whenDeleteMissing() {
        when(sectionRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> sectionService.deleteSection(5L))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Section not found");
    }
}
