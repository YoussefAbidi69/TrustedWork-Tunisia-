package tn.esprit.community.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.lms.BlockDTO;
import tn.esprit.community.dto.lms.CourseDTO;
import tn.esprit.community.dto.lms.SectionDTO;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.dto.response.CourseResponse;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.entity.enums.BlockType;
import tn.esprit.community.service.impl.LearningCourseServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningCourseServiceImplTest {

    @Mock private CourseService courseService;
    @Mock private SectionService sectionService;
    @Mock private BlockService blockService;

    @InjectMocks
    private LearningCourseServiceImpl learningCourseService;

    @Test
    @DisplayName("shouldMapCourse_whenGetCourse")
    void shouldMapCourse_whenGetCourse() {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java")
                .description("Desc")
                .authorId(9L)
                .communityId(2L)
                .published(true)
                .build();
        when(courseService.getCourse(1L)).thenReturn(response);

        CourseDTO dto = learningCourseService.getCourse(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Java");
        assertThat(dto.isPublished()).isTrue();
    }

    @Test
    @DisplayName("shouldPassThroughRequest_whenCreateCourse")
    void shouldPassThroughRequest_whenCreateCourse() {
        CourseResponse saved = CourseResponse.builder().id(2L).title("T").build();
        when(courseService.createCourse(any(CourseRequest.class))).thenReturn(saved);

        CourseDTO dto = CourseDTO.builder()
                .title("T")
                .description("D")
                .authorId(7L)
                .communityId(3L)
                .published(false)
                .build();

        CourseDTO response = learningCourseService.createCourse(dto);

        ArgumentCaptor<CourseRequest> captor = ArgumentCaptor.forClass(CourseRequest.class);
        verify(courseService).createCourse(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("T");
        assertThat(response.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("shouldMapSections_whenListingSections")
    void shouldMapSections_whenListingSections() {
        SectionResponse s1 = SectionResponse.builder().id(1L).courseId(4L).title("S1").orderIndex(0).build();
        when(sectionService.listSections(4L)).thenReturn(List.of(s1));

        List<SectionDTO> result = learningCourseService.listSections(4L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("S1");
    }

    @Test
    @DisplayName("shouldMapBlock_whenCreateBlock")
    void shouldMapBlock_whenCreateBlock() {
        BlockResponse block = BlockResponse.builder()
                .id(5L)
                .sectionId(3L)
                .title("B")
                .content("C")
                .fileUrl("u")
                .orderIndex(1)
                .type(BlockType.TEXT)
                .build();
        when(blockService.createBlock(any(Long.class), any())).thenReturn(block);

        BlockDTO response = learningCourseService.createBlock(3L, BlockDTO.builder().title("B").build());

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getSectionId()).isEqualTo(3L);
    }
}
