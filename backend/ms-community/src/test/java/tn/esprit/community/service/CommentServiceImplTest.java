package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.CommentRequest;
import tn.esprit.community.dto.response.CommentResponse;
import tn.esprit.community.entity.Comment;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.Post;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CommentRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.service.impl.CommentServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CommentServiceImpl.
 * Covers: null-ID validation, adding comment to post/course, listing, delete.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    // --- addComment (to Post) ---

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenPostIdIsNull")
    void shouldThrowPostNotFoundException_whenPostIdIsNull() {
        CommentRequest req = CommentRequest.builder().content("Hello").userId(1L).build();
        assertThatThrownBy(() -> commentService.addComment(null, req))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("Post ID cannot be null");
    }

    @Test
    @DisplayName("shouldSaveAndReturnCommentResponse_whenPostIdIsValid")
    void shouldSaveAndReturnCommentResponse_whenPostIdIsValid() {
        CommentRequest req = CommentRequest.builder().content("Nice post!").userId(7L).build();
        Post post = Post.builder().id(1L).build();
        when(postRepository.getReferenceById(1L)).thenReturn(post);

        Comment saved = Comment.builder().id(10L).content("Nice post!").userId(7L).post(post).build();
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse response = commentService.addComment(1L, req);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getContent()).isEqualTo("Nice post!");
        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(7L);
    }

    // --- listComments (by Post) ---

    @Test
    @DisplayName("shouldReturnCommentsOrderedById_whenListingByPostId")
    void shouldReturnCommentsOrderedById_whenListingByPostId() {
        Post post = Post.builder().id(1L).build();
        Comment c1 = Comment.builder().id(1L).content("A").userId(1L).post(post).build();
        Comment c2 = Comment.builder().id(2L).content("B").userId(2L).post(post).build();
        when(commentRepository.findByPostIdOrderByIdAsc(1L)).thenReturn(List.of(c1, c2));

        List<CommentResponse> result = commentService.listComments(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    // --- addCommentToCourse ---

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseIdIsNull")
    void shouldThrowLearningNotFoundException_whenCourseIdIsNull() {
        CommentRequest req = CommentRequest.builder().content("Great course!").userId(3L).build();
        assertThatThrownBy(() -> commentService.addCommentToCourse(null, req))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Course ID cannot be null");
    }

    @Test
    @DisplayName("shouldSaveAndReturnCommentResponse_whenCourseIdIsValid")
    void shouldSaveAndReturnCommentResponse_whenCourseIdIsValid() {
        CommentRequest req = CommentRequest.builder().content("Excellent!").userId(5L).build();
        Course course = Course.builder().id(2L).build();
        when(courseRepository.getReferenceById(2L)).thenReturn(course);

        Comment saved = Comment.builder().id(20L).content("Excellent!").userId(5L).course(course).build();
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse response = commentService.addCommentToCourse(2L, req);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getContent()).isEqualTo("Excellent!");
        // postId should be null since it's a course comment
        assertThat(response.getPostId()).isNull();
    }

    // --- listCommentsByCourse ---

    @Test
    @DisplayName("shouldReturnCourseComments_whenListingByCourseId")
    void shouldReturnCourseComments_whenListingByCourseId() {
        Course course = Course.builder().id(2L).build();
        Comment c = Comment.builder().id(5L).content("Great").userId(1L).course(course).build();
        when(commentRepository.findByCourseIdOrderByIdAsc(2L)).thenReturn(List.of(c));

        List<CommentResponse> result = commentService.listCommentsByCourse(2L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Great");
    }

    // --- deleteComment ---

    @Test
    @DisplayName("shouldCallDeleteById_whenDeletingComment")
    void shouldCallDeleteById_whenDeletingComment() {
        commentService.deleteComment(15L);
        verify(commentRepository).deleteById(15L);
    }
}
