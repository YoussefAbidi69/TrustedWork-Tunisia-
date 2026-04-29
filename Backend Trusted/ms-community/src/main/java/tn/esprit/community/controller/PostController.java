package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.PostRequest;
import tn.esprit.community.dto.response.PostResponse;
import tn.esprit.community.service.PostService;
import tn.esprit.community.entity.enums.PostStatus;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest postRequest) {
        PostResponse createdPost = postService.createPost(postRequest);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(
            @PathVariable Long id, @RequestParam(required = false) Long voterId) {
        PostResponse post = postService.getPost(id, voterId);
        return new ResponseEntity<>(post, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @RequestBody PostRequest postRequest) {
        PostResponse updatedPost = postService.updatePost(id, postRequest);
        return new ResponseEntity<>(updatedPost, HttpStatus.OK);
    }

    @PostMapping("/{postId}/publish")
    public ResponseEntity<PostResponse> publishPost(@PathVariable Long postId) {
        PostResponse publishedPost = postService.publishPost(postId);
        return new ResponseEntity<>(publishedPost, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, @RequestParam Long userId) {
        postService.deletePost(id, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> listPosts(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Long voterId) {
        List<PostResponse> posts = postService.listPosts(communityId, status, voterId);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
}
