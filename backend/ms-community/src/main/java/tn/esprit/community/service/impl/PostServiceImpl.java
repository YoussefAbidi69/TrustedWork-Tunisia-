package tn.esprit.community.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.PostDTO;
import tn.esprit.community.entity.Enum.VoteType;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Vote;
import tn.esprit.community.exception.PostDeleteForbiddenException;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.mapper.PostMapper;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.PostType;
import tn.esprit.community.repository.CommunityRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.VoteRepository;
import tn.esprit.community.service.PostService;
import tn.esprit.community.entity.Enum.ValidationResult;
import tn.esprit.community.service.ValidationService;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final CommunityRepository communityRepository;
    private final PostMapper postMapper;
    private final ValidationService validationService;
    private final VoteRepository voteRepository;

    public PostServiceImpl(
            PostRepository postRepository,
            CommunityRepository communityRepository,
            PostMapper postMapper,
            ValidationService validationService,
            VoteRepository voteRepository) {
        this.postRepository = postRepository;
        this.communityRepository = communityRepository;
        this.postMapper = postMapper;
        this.validationService = validationService;
        this.voteRepository = voteRepository;
    }

    @Override
    public PostDTO createPost(PostDTO postDTO) {
        Post post = postMapper.toEntity(postDTO);
        post.setCommunity(communityRepository.getReferenceById(postDTO.getCommunityId()));
        PostDTO dto = postMapper.toDto(postRepository.save(post));
        enrichVoteCounts(dto);
        return dto;
    }

    @Override
    public PostDTO getPost(Long id, Long voterId) {
        PostDTO dto = postMapper.toDto(
                postRepository.findById(id).orElseThrow(() -> new PostNotFoundException("Post not found")));
        enrichVoteCounts(dto);
        enrichMyVote(dto, voterId);
        return dto;
    }

    @Override
    public PostDTO updatePost(Long id, PostDTO postDTO) {
        postRepository.findById(id).orElseThrow(() -> new PostNotFoundException("Post not found"));
        Post updated = postMapper.toEntity(postDTO);
        updated.setId(id);
        updated.setCommunity(communityRepository.getReferenceById(postDTO.getCommunityId()));
        PostDTO dto = postMapper.toDto(postRepository.save(updated));
        enrichVoteCounts(dto);
        return dto;
    }

    @Override
    public PostDTO publishPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Post not found"));
        if (validationService.validate(post) == ValidationResult.REJECTED) {
            throw new tn.esprit.community.exception.ValidationException("Post rejected by validation rules");
        }
        post.setStatus(PostStatus.PUBLISHED);
        PostDTO dto = postMapper.toDto(postRepository.save(post));
        enrichVoteCounts(dto);
        return dto;
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
    public List<PostDTO> listPosts(Long communityId, PostType type, PostStatus status, Long voterId) {
        List<PostDTO> list = postRepository.findAll().stream()
                .filter(p -> communityId == null
                        || (p.getCommunity() != null && communityId.equals(p.getCommunity().getId())))
                .filter(p -> type == null || type == p.getType())
                .filter(p -> status == null || status == p.getStatus())
                .map(postMapper::toDto)
                .peek(this::enrichVoteCounts)
                .collect(Collectors.toList());
        enrichMyVotes(list, voterId);
        return list;
    }

    private void enrichVoteCounts(PostDTO dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }
        long up = voteRepository.countByPost_IdAndType(dto.getId(), VoteType.UP);
        long down = voteRepository.countByPost_IdAndType(dto.getId(), VoteType.DOWN);
        dto.setUpvoteCount((int) Math.min(up, Integer.MAX_VALUE));
        dto.setDownvoteCount((int) Math.min(down, Integer.MAX_VALUE));
    }

    private void enrichMyVote(PostDTO dto, Long voterId) {
        if (dto == null || dto.getId() == null || voterId == null) {
            return;
        }
        voteRepository.findByPost_IdAndUserId(dto.getId(), voterId).ifPresent(v -> dto.setMyVote(v.getType()));
    }

    private void enrichMyVotes(List<PostDTO> dtos, Long voterId) {
        if (voterId == null || dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream()
                .map(PostDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<Vote> votes = voteRepository.findByUserIdAndPost_IdIn(voterId, ids);
        Map<Long, VoteType> byPostId =
                votes.stream().collect(Collectors.toMap(v -> v.getPost().getId(), Vote::getType, (a, b) -> a));
        for (PostDTO dto : dtos) {
            if (dto.getId() != null && byPostId.containsKey(dto.getId())) {
                dto.setMyVote(byPostId.get(dto.getId()));
            }
        }
    }
}
