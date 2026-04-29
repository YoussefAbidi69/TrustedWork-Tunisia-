package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.PostRequest;
import tn.esprit.community.dto.response.PostResponse;
import tn.esprit.community.entity.Community;
import tn.esprit.community.entity.enums.PostStatus;
import tn.esprit.community.entity.enums.VoteType;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Vote;
import tn.esprit.community.exception.PostDeleteForbiddenException;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.VoteRepository;
import tn.esprit.community.service.impl.PostServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostServiceImpl.
 * Covers: creation (default DRAFT + explicit PUBLISHED), Discord notification,
 * delete permission guard, publishPost, listPosts branches, vote enrichment.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private DiscordNotificationService discordNotificationService;

    @InjectMocks
    private PostServiceImpl postService;

    private Community community(Long id) {
        return Community.builder().id(id).name("Tech").build();
    }

    private Post post(Long id, Long createdBy, PostStatus status, Community community) {
        return Post.builder()
                .id(id).title("T" + id).content("C").createdBy(createdBy)
                .status(status).community(community).reportCount(0).build();
    }

    // --- createPost ---

    @Test
    @DisplayName("shouldCreatePostWithDraftStatus_whenNoStatusProvided")
    void shouldCreatePostWithDraftStatus_whenNoStatusProvided() {
        Community comm = community(1L);
        when(communityRepository.findById(1L)).thenReturn(Optional.of(comm));
        Post saved = post(1L, 10L, PostStatus.DRAFT, comm);
        when(postRepository.save(any())).thenReturn(saved);
        when(voteRepository.countByPostIdAndType(1L, VoteType.UP)).thenReturn(0L);
        when(voteRepository.countByPostIdAndType(1L, VoteType.DOWN)).thenReturn(0L);

        PostResponse r = postService.createPost(PostRequest.builder()
                .title("Hello").communityId(1L).createdBy(10L).build());

        assertThat(r.getStatus()).isEqualTo(PostStatus.DRAFT);
        verify(discordNotificationService, never()).notifyPostPublished(any());
    }

    @Test
    @DisplayName("shouldNotifyDiscord_whenPostCreatedWithPublishedStatus")
    void shouldNotifyDiscord_whenPostCreatedWithPublishedStatus() {
        Community comm = community(1L);
        when(communityRepository.findById(1L)).thenReturn(Optional.of(comm));
        Post saved = post(2L, 5L, PostStatus.PUBLISHED, comm);
        when(postRepository.save(any())).thenReturn(saved);
        when(voteRepository.countByPostIdAndType(2L, VoteType.UP)).thenReturn(0L);
        when(voteRepository.countByPostIdAndType(2L, VoteType.DOWN)).thenReturn(0L);

        postService.createPost(PostRequest.builder()
                .title("News").communityId(1L).createdBy(5L).status(PostStatus.PUBLISHED).build());

        verify(discordNotificationService).notifyPostPublished(any());
    }

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenCommunityNotFoundOnCreate")
    void shouldThrowPostNotFoundException_whenCommunityNotFoundOnCreate() {
        when(communityRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.createPost(
                PostRequest.builder().communityId(99L).build()))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("Community not found");
    }

    // --- getPost ---

    @Test
    @DisplayName("shouldReturnPostWithVoteCounts_whenPostExists")
    void shouldReturnPostWithVoteCounts_whenPostExists() {
        Post p = post(3L, 10L, PostStatus.PUBLISHED, community(1L));
        when(postRepository.findById(3L)).thenReturn(Optional.of(p));
        when(voteRepository.countByPostIdAndType(3L, VoteType.UP)).thenReturn(5L);
        when(voteRepository.countByPostIdAndType(3L, VoteType.DOWN)).thenReturn(1L);

        PostResponse r = postService.getPost(3L, null);

        assertThat(r.getUpvoteCount()).isEqualTo(5L);
        assertThat(r.getDownvoteCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldPopulateMyVote_whenVoterHasVoted")
    void shouldPopulateMyVote_whenVoterHasVoted() {
        Post p = post(3L, 10L, PostStatus.PUBLISHED, community(1L));
        when(postRepository.findById(3L)).thenReturn(Optional.of(p));
        when(voteRepository.countByPostIdAndType(any(), any())).thenReturn(0L);
        Vote vote = Vote.builder().id(1L).post(p).userId(42L).type(VoteType.UP).build();
        when(voteRepository.findByPostIdAndUserId(3L, 42L)).thenReturn(Optional.of(vote));

        PostResponse r = postService.getPost(3L, 42L);
        assertThat(r.getMyVote()).isEqualTo(VoteType.UP);
    }

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenPostNotFoundOnGet")
    void shouldThrowPostNotFoundException_whenPostNotFoundOnGet() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.getPost(99L, null))
                .isInstanceOf(PostNotFoundException.class);
    }

    // --- updatePost ---

    @Test
    @DisplayName("shouldUpdatePostFields_whenProvidedInRequest")
    void shouldUpdatePostFields_whenProvidedInRequest() {
        Community comm = community(1L);
        Post existing = post(4L, 10L, PostStatus.DRAFT, comm);
        when(postRepository.findById(4L)).thenReturn(Optional.of(existing));

        Community newComm = community(2L);
        when(communityRepository.findById(2L)).thenReturn(Optional.of(newComm));

        Post updated = post(4L, 10L, PostStatus.PUBLISHED, newComm);
        updated.setTitle("New Title");
        updated.setContent("New Content");
        when(postRepository.save(existing)).thenReturn(updated);

        when(voteRepository.countByPostIdAndType(any(), any())).thenReturn(0L);

        PostRequest request = PostRequest.builder()
                .title("New Title")
                .content("New Content")
                .status(PostStatus.PUBLISHED)
                .communityId(2L)
                .build();

        PostResponse r = postService.updatePost(4L, request);

        assertThat(r.getTitle()).isEqualTo("New Title");
        assertThat(r.getContent()).isEqualTo("New Content");
        assertThat(r.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(r.getCommunityId()).isEqualTo(2L);
        verify(postRepository).save(existing);
    }

    // --- deletePost ---

    @Test
    @DisplayName("shouldDeletePost_whenCalledByAuthor")
    void shouldDeletePost_whenCalledByAuthor() {
        Post p = post(4L, 10L, PostStatus.DRAFT, community(1L));
        when(postRepository.findById(4L)).thenReturn(Optional.of(p));
        postService.deletePost(4L, 10L);
        verify(postRepository).delete(p);
    }

    @Test
    @DisplayName("shouldThrowPostDeleteForbiddenException_whenNonAuthorTriesToDelete")
    void shouldThrowPostDeleteForbiddenException_whenNonAuthorTriesToDelete() {
        Post p = post(4L, 10L, PostStatus.DRAFT, community(1L));
        when(postRepository.findById(4L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> postService.deletePost(4L, 99L))
                .isInstanceOf(PostDeleteForbiddenException.class)
                .hasMessageContaining("Only the author");
        verify(postRepository, never()).delete(any());
    }

    // --- publishPost ---

    @Test
    @DisplayName("shouldSetStatusPublishedAndNotifyDiscord_whenPublishPostCalled")
    void shouldSetStatusPublishedAndNotifyDiscord_whenPublishPostCalled() {
        Community comm = community(1L);
        Post draft = post(5L, 10L, PostStatus.DRAFT, comm);
        when(postRepository.findById(5L)).thenReturn(Optional.of(draft));
        Post published = post(5L, 10L, PostStatus.PUBLISHED, comm);
        when(postRepository.save(draft)).thenReturn(published);
        when(voteRepository.countByPostIdAndType(5L, VoteType.UP)).thenReturn(0L);
        when(voteRepository.countByPostIdAndType(5L, VoteType.DOWN)).thenReturn(0L);

        PostResponse r = postService.publishPost(5L);
        assertThat(r.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        verify(discordNotificationService).notifyPostPublished(any());
    }

    // --- listPosts branches ---

    @Test
    @DisplayName("shouldReturnPostsByCommunityAndStatus_whenBothFiltersProvided")
    void shouldReturnPostsByCommunityAndStatus_whenBothFiltersProvided() {
        Community comm = community(1L);
        when(postRepository.findByCommunityIdAndStatusOrderByIdDesc(1L, PostStatus.PUBLISHED))
                .thenReturn(List.of(post(1L, 10L, PostStatus.PUBLISHED, comm)));
        when(voteRepository.countByPostIdAndType(any(), any())).thenReturn(0L);

        List<PostResponse> result = postService.listPosts(1L, PostStatus.PUBLISHED, null);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnAllPostsForCommunity_whenOnlyCommunityIdProvided")
    void shouldReturnAllPostsForCommunity_whenOnlyCommunityIdProvided() {
        Community comm = community(2L);
        when(postRepository.findByCommunityIdOrderByIdDesc(2L))
                .thenReturn(List.of(post(1L, 10L, PostStatus.DRAFT, comm)));
        when(voteRepository.countByPostIdAndType(any(), any())).thenReturn(0L);

        assertThat(postService.listPosts(2L, null, null)).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnPostsByStatusOnly_whenOnlyStatusProvided")
    void shouldReturnPostsByStatusOnly_whenOnlyStatusProvided() {
        Community comm = community(1L);
        when(postRepository.findByStatus(PostStatus.HIDDEN))
                .thenReturn(List.of(post(1L, 10L, PostStatus.HIDDEN, comm)));
        when(voteRepository.countByPostIdAndType(any(), any())).thenReturn(0L);

        List<PostResponse> result = postService.listPosts(null, PostStatus.HIDDEN, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PostStatus.HIDDEN);
    }

    @Test
    @DisplayName("shouldReturnAllPosts_whenNoFiltersProvided")
    void shouldReturnAllPosts_whenNoFiltersProvided() {
        Community comm = community(1L);
        when(postRepository.findAll()).thenReturn(List.of(
                post(1L, 10L, PostStatus.DRAFT, comm),
                post(2L, 11L, PostStatus.PUBLISHED, comm)));
        when(voteRepository.countByPostIdAndType(any(), any())).thenReturn(0L);

        assertThat(postService.listPosts(null, null, null)).hasSize(2);
    }
}
