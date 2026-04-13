package tn.esprit.community.post;

import java.util.List;
import tn.esprit.community.dto.PostDTO;

public interface PostService {
    PostDTO createPost(PostDTO postDTO);
    PostDTO getPost(Long id);
    PostDTO updatePost(Long id, PostDTO postDTO);
    PostDTO publishPost(Long postId);
    List<PostDTO> listPosts(Long communityId, PostType type, PostStatus status);
}
