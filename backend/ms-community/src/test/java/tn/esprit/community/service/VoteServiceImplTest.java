package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.VoteRequest;
import tn.esprit.community.dto.response.VoteResponse;
import tn.esprit.community.entity.Enum.VoteType;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Vote;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.VoteRepository;
import tn.esprit.community.service.impl.VoteServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VoteServiceImpl.
 *
 * The vote toggle has three distinct paths:
 *  1. Same vote type already exists → delete (un-vote) → return response with null type
 *  2. Different vote type already exists → update type → return updated response
 *  3. No existing vote → create new → return new response
 */
@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock private VoteRepository voteRepository;
    @Mock private PostRepository postRepository;

    @InjectMocks
    private VoteServiceImpl voteService;

    private Post post(Long id) {
        return Post.builder().id(id).build();
    }

    // --- Path 1: same type → delete (un-vote) ---

    @Test
    @DisplayName("shouldDeleteVoteAndReturnNullType_whenSameVoteTypeSubmittedAgain")
    void shouldDeleteVoteAndReturnNullType_whenSameVoteTypeSubmittedAgain() {
        Post post = post(1L);
        Vote existing = Vote.builder().id(5L).post(post).userId(10L).type(VoteType.UP).build();
        when(voteRepository.findByPost_IdAndUserId(1L, 10L)).thenReturn(Optional.of(existing));

        VoteRequest request = VoteRequest.builder().userId(10L).type(VoteType.UP).build();
        VoteResponse response = voteService.vote(1L, request);

        verify(voteRepository).delete(existing);
        assertThat(response.getType()).isNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(10L);
    }

    // --- Path 2: different type → update ---

    @Test
    @DisplayName("shouldUpdateVoteType_whenDifferentVoteTypeSubmitted")
    void shouldUpdateVoteType_whenDifferentVoteTypeSubmitted() {
        Post post = post(1L);
        Vote existing = Vote.builder().id(5L).post(post).userId(10L).type(VoteType.UP).build();
        when(voteRepository.findByPost_IdAndUserId(1L, 10L)).thenReturn(Optional.of(existing));

        Vote updated = Vote.builder().id(5L).post(post).userId(10L).type(VoteType.DOWN).build();
        when(voteRepository.save(existing)).thenReturn(updated);

        VoteRequest request = VoteRequest.builder().userId(10L).type(VoteType.DOWN).build();
        VoteResponse response = voteService.vote(1L, request);

        assertThat(response.getType()).isEqualTo(VoteType.DOWN);
        assertThat(response.getId()).isEqualTo(5L);
        verify(voteRepository).save(existing);
        verify(voteRepository, never()).delete(any());
    }

    // --- Path 3: no existing vote → create ---

    @Test
    @DisplayName("shouldCreateNewVote_whenNoExistingVoteForUser")
    void shouldCreateNewVote_whenNoExistingVoteForUser() {
        when(voteRepository.findByPost_IdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        Post post = post(1L);
        when(postRepository.getReferenceById(1L)).thenReturn(post);

        Vote saved = Vote.builder().id(99L).post(post).userId(20L).type(VoteType.UP).build();
        when(voteRepository.save(any(Vote.class))).thenReturn(saved);

        VoteRequest request = VoteRequest.builder().userId(20L).type(VoteType.UP).build();
        VoteResponse response = voteService.vote(1L, request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getType()).isEqualTo(VoteType.UP);
        assertThat(response.getUserId()).isEqualTo(20L);
        verify(voteRepository).save(any(Vote.class));
    }
}
