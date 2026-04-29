package tn.esprit.community.service;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.CourseCommentRequest;
import tn.esprit.community.dto.response.CourseCommentResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.CourseComment;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CourseCommentRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.service.impl.CourseCommentServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCommentServiceImplTest {

    @Mock private CourseCommentRepository commentRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private CourseCommentServiceImpl commentService;

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenCourseIdIsNull")
    void shouldThrowPostNotFoundException_whenCourseIdIsNull() {
        CourseCommentRequest request = CourseCommentRequest.builder().content("Hi").userId(1L).build();

        assertThatThrownBy(() -> commentService.addComment(null, request))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("Course ID cannot be null");
    }

    @Test
    @DisplayName("shouldSaveComment_whenCourseIdIsValid")
    void shouldSaveComment_whenCourseIdIsValid() {
        CourseCommentRequest request = CourseCommentRequest.builder().content("Nice").userId(3L).build();
        Course course = Course.builder().id(7L).build();
        when(courseRepository.getReferenceById(7L)).thenReturn(course);

        CourseComment saved = CourseComment.builder().id(11L).course(course).content("Nice").userId(3L).build();
        when(commentRepository.save(any(CourseComment.class))).thenReturn(saved);

        CourseCommentResponse response = commentService.addComment(7L, request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getCourseId()).isEqualTo(7L);
        assertThat(response.getContent()).isEqualTo("Nice");
    }

    @Test
    @DisplayName("shouldReturnComments_whenListingByCourse")
    void shouldReturnComments_whenListingByCourse() {
        Course course = Course.builder().id(2L).build();
        CourseComment c1 = CourseComment.builder().id(1L).course(course).content("A").userId(1L).build();
        CourseComment c2 = CourseComment.builder().id(2L).course(course).content("B").userId(2L).build();
        when(commentRepository.findByCourseIdOrderByIdAsc(2L)).thenReturn(List.of(c1, c2));

        List<CourseCommentResponse> responses = commentService.listComments(2L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getContent()).isEqualTo("A");
        assertThat(responses.get(1).getContent()).isEqualTo("B");
    }

    @Test
    @DisplayName("shouldDeleteComment_whenCalled")
    void shouldDeleteComment_whenCalled() {
        commentService.deleteComment(4L);

        verify(commentRepository).deleteById(4L);
    }
}
