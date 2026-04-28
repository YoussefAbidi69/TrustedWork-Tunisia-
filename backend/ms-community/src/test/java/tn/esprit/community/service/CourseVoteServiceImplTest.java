package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.CourseVoteRequest;
import tn.esprit.community.dto.response.CourseVoteResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.CourseVote;
import tn.esprit.community.entity.enums.VoteType;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.CourseVoteRepository;
import tn.esprit.community.service.impl.CourseVoteServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CourseVoteServiceImpl.
 *
 * Mirrors the same three-path toggle logic as VoteServiceImpl, but for courses:
 *  1. Same vote type again → delete (un-vote)
 *  2. Different vote type → update
 *  3. No existing vote → create new
 */
@ExtendWith(MockitoExtension.class)
class CourseVoteServiceImplTest {

    @Mock private CourseVoteRepository voteRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private CourseVoteServiceImpl courseVoteService;

    private Course course(Long id) {
        return Course.builder().id(id).title("C" + id).build();
    }

    // --- Path 1: same type → delete ---

    @Test
    @DisplayName("shouldDeleteCourseVoteAndReturnNullType_whenSameVoteTypeResubmitted")
    void shouldDeleteCourseVoteAndReturnNullType_whenSameVoteTypeResubmitted() {
        Course course = course(1L);
        CourseVote existing = CourseVote.builder().id(3L).course(course).userId(10L).type(VoteType.UP).build();
        when(voteRepository.findByCourseIdAndUserId(1L, 10L)).thenReturn(Optional.of(existing));

        CourseVoteRequest request = CourseVoteRequest.builder().userId(10L).type(VoteType.UP).build();
        CourseVoteResponse response = courseVoteService.vote(1L, request);

        verify(voteRepository).delete(existing);
        assertThat(response.getType()).isNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getCourseId()).isEqualTo(1L);
    }

    // --- Path 2: different type → update ---

    @Test
    @DisplayName("shouldUpdateCourseVoteType_whenDifferentVoteTypeSubmitted")
    void shouldUpdateCourseVoteType_whenDifferentVoteTypeSubmitted() {
        Course course = course(1L);
        CourseVote existing = CourseVote.builder().id(3L).course(course).userId(10L).type(VoteType.UP).build();
        when(voteRepository.findByCourseIdAndUserId(1L, 10L)).thenReturn(Optional.of(existing));

        CourseVote saved = CourseVote.builder().id(3L).course(course).userId(10L).type(VoteType.DOWN).build();
        when(voteRepository.save(existing)).thenReturn(saved);

        CourseVoteRequest request = CourseVoteRequest.builder().userId(10L).type(VoteType.DOWN).build();
        CourseVoteResponse response = courseVoteService.vote(1L, request);

        assertThat(response.getType()).isEqualTo(VoteType.DOWN);
        assertThat(response.getId()).isEqualTo(3L);
        verify(voteRepository).save(existing);
        verify(voteRepository, never()).delete(any());
    }

    // --- Path 3: new vote ---

    @Test
    @DisplayName("shouldCreateNewCourseVote_whenNoExistingVoteForUser")
    void shouldCreateNewCourseVote_whenNoExistingVoteForUser() {
        when(voteRepository.findByCourseIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        Course course = course(1L);
        when(courseRepository.getReferenceById(1L)).thenReturn(course);

        CourseVote saved = CourseVote.builder().id(77L).course(course).userId(20L).type(VoteType.DOWN).build();
        when(voteRepository.save(any(CourseVote.class))).thenReturn(saved);

        CourseVoteRequest request = CourseVoteRequest.builder().userId(20L).type(VoteType.DOWN).build();
        CourseVoteResponse response = courseVoteService.vote(1L, request);

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getType()).isEqualTo(VoteType.DOWN);
        assertThat(response.getUserId()).isEqualTo(20L);
        verify(voteRepository).save(any(CourseVote.class));
    }
}
