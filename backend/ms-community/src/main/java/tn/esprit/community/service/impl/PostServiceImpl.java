package tn.esprit.community.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.request.PostRequest;
import tn.esprit.community.dto.response.PostResponse;
import tn.esprit.community.entity.Community;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.VoteType;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Vote;
import tn.esprit.community.exception.PostDeleteForbiddenException;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.VoteRepository;
import tn.esprit.community.service.DiscordNotificationService;
import tn.esprit.community.service.PostService;

@Service
public class PostServiceImpl implements PostService {
    private static final String POST_NOT_FOUND = "Post not found";

    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;
    private final VoteRepository voteRepository;
    private final DiscordNotificationService discordNotificationService;

    public PostServiceImpl(
            PostRepository postRepository,
            CommunityRepository communityRepository,
            VoteRepository voteRepository,
            DiscordNotificationService discordNotificationService) {
        this.postRepository = postRepository;
        this.communityRepository = communityRepository;
        this.voteRepository = voteRepository;
        this.discordNotificationService = discordNotificationService;
    }

    @Override
    public PostResponse createPost(PostRequest postRequest) {
        Community community = communityRepository
                .findById(postRequest.getCommunityId())
                .orElseThrow(() -> new PostNotFoundException("Community not found"));

        Post post = Post.builder()
                .title(postRequest.getTitle())
                .content(postRequest.getContent())
                .createdBy(postRequest.getCreatedBy())
                .community(community)
                .status(postRequest.getStatus() == null ? PostStatus.DRAFT : postRequest.getStatus())
                .reportCount(0)
                .build();

        PostResponse response = toPostResponse(postRepository.save(post), null);
        if (post.getStatus() == PostStatus.PUBLISHED) {
            discordNotificationService.notifyPostPublished(response);
        }
        return response;
    }

    @Override
    public PostResponse getPost(Long id, Long voterId) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
        return toPostResponse(post, voterId);
    }

    @Override
    public PostResponse updatePost(Long id, PostRequest postRequest) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));

        if (postRequest.getTitle() != null) {
            post.setTitle(postRequest.getTitle());
        }
        if (postRequest.getContent() != null) {
            post.setContent(postRequest.getContent());
        }
        if (postRequest.getStatus() != null) {
            post.setStatus(postRequest.getStatus());
        }
        if (postRequest.getCommunityId() != null) {
            Community community = communityRepository
                    .findById(postRequest.getCommunityId())
                    .orElseThrow(() -> new PostNotFoundException("Community not found"));
            post.setCommunity(community);
        }

        return toPostResponse(postRepository.save(post), null);
    }

    @Override
    public PostResponse publishPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(POST_NOT_FOUND));
        post.setStatus(PostStatus.PUBLISHED);
        PostResponse response = toPostResponse(postRepository.save(post), null);
        discordNotificationService.notifyPostPublished(response);
        return response;
    }

    @Override
    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException("Post not found"));
        if (post.getCreatedBy() == null || !post.getCreatedBy().equals(userId)) {
            throw new PostDeleteForbiddenException("Only the author can delete this post");
        }
        postRepository.delete(post);
    }

    @Override
    public List<PostResponse> listPosts(Long communityId, PostStatus status, Long voterId) {
        List<Post> posts;
        if (communityId != null && status != null) {
            posts = postRepository.findByCommunity_IdAndStatusOrderByIdDesc(communityId, status);
        } else if (communityId != null) {
            posts = postRepository.findByCommunity_IdOrderByIdDesc(communityId);
        } else if (status != null) {
            posts = postRepository.findByStatus(status);
        } else {
            posts = postRepository.findAll();
        }

        List<PostResponse> responses = new ArrayList<>(posts.size());
        for (Post post : posts) {
            responses.add(toPostResponse(post, null));
        }
        enrichMyVotes(responses, voterId);
        return responses;
    }

    private PostResponse toPostResponse(Post post, Long voterId) {
        long up = voteRepository.countByPost_IdAndType(post.getId(), VoteType.UP);
        long down = voteRepository.countByPost_IdAndType(post.getId(), VoteType.DOWN);

        PostResponse response = PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .createdBy(post.getCreatedBy())
                .communityId(post.getCommunity() != null ? post.getCommunity().getId() : null)
                .status(post.getStatus())
                .reportCount(post.getReportCount())
                .upvoteCount(up)
                .downvoteCount(down)
                .build();

        if (voterId != null) {
            voteRepository.findByPost_IdAndUserId(post.getId(), voterId).ifPresent(v -> response.setMyVote(v.getType()));
        }
        return response;
    }

    private void enrichMyVotes(List<PostResponse> dtos, Long voterId) {
        if (voterId == null || dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream()
                .map(PostResponse::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<Vote> votes = voteRepository.findByUserIdAndPost_IdIn(voterId, ids);
        Map<Long, VoteType> byPostId =
                votes.stream().collect(Collectors.toMap(v -> v.getPost().getId(), Vote::getType, (a, b) -> a));
        for (PostResponse dto : dtos) {
            if (dto.getId() != null && byPostId.containsKey(dto.getId())) {
                dto.setMyVote(byPostId.get(dto.getId()));
            }
        }
    }
}
