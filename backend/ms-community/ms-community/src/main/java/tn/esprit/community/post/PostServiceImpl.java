package tn.esprit.community.post;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.PostDTO;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.mapper.PostMapper;
import tn.esprit.community.validation.ValidationResult;
import tn.esprit.community.validation.ValidationService;

@Service
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final ValidationService validationService;

    public PostServiceImpl(PostRepository postRepository, PostMapper postMapper, ValidationService validationService) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.validationService = validationService;
    }

    @Override
    public PostDTO createPost(PostDTO postDTO) {
        Post post = postMapper.toEntity(postDTO);
        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    public PostDTO getPost(Long id) {
        return postMapper.toDto(postRepository.findById(id).orElseThrow(() -> new PostNotFoundException("Post not found")));
    }

    @Override
    public PostDTO updatePost(Long id, PostDTO postDTO) {
        Post existing = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException("Post not found"));
        Post updated = postMapper.toEntity(postDTO);
        updated.setId(existing.getId());
        return postMapper.toDto(postRepository.save(updated));
    }

    @Override
    public PostDTO publishPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Post not found"));
        if (validationService.validate(post) == ValidationResult.REJECTED) {
            throw new tn.esprit.community.exception.ValidationException("Post rejected by validation rules");
        }
        post.setStatus(PostStatus.PUBLISHED);
        return postMapper.toDto(postRepository.save(post));
    }

    @Override
    public List<PostDTO> listPosts(Long communityId, PostType type, PostStatus status) {
        return postRepository.findAll().stream()
                .filter(p -> communityId == null || communityId.equals(p.getCommunityId()))
                .filter(p -> type == null || type == p.getType())
                .filter(p -> status == null || status == p.getStatus())
                .map(postMapper::toDto)
                .collect(Collectors.toList());
    }
}
