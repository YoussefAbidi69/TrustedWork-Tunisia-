package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.PostRequest;
import tn.esprit.community.dto.response.PostResponse;
import tn.esprit.community.entity.enums.PostStatus;

public interface PostService {
    PostResponse createPost(PostRequest postRequest);

    PostResponse getPost(Long id, Long voterId);

    PostResponse updatePost(Long id, PostRequest postRequest);

    PostResponse publishPost(Long postId);

    void deletePost(Long id, Long userId);

    List<PostResponse> listPosts(Long communityId, PostStatus status, Long voterId);
}
