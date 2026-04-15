package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.PostDTO;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.PostType;

public interface PostService {
    PostDTO createPost(PostDTO postDTO);

    PostDTO getPost(Long id, Long voterId);

    PostDTO updatePost(Long id, PostDTO postDTO);
    PostDTO publishPost(Long postId);
    void deletePost(Long id, Long userId);
    List<PostDTO> listPosts(Long communityId, PostType type, PostStatus status, Long voterId);
}
