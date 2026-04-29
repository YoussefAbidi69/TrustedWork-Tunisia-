package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.service.CommentService;
import tn.esprit.community.dto.request.CommentRequest;
import tn.esprit.community.dto.response.CommentResponse;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId, @RequestBody CommentRequest commentRequest) {
        CommentResponse addedComment = commentService.addComment(postId, commentRequest);
        return new ResponseEntity<>(addedComment, HttpStatus.CREATED);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponse>> listComments(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.listComments(postId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<CommentResponse> addCommentToCourse(
            @PathVariable Long courseId, @RequestBody CommentRequest commentRequest) {
        CommentResponse addedComment = commentService.addCommentToCourse(courseId, commentRequest);
        return new ResponseEntity<>(addedComment, HttpStatus.CREATED);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CommentResponse>> listCommentsByCourse(@PathVariable Long courseId) {
        List<CommentResponse> comments = commentService.listCommentsByCourse(courseId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
