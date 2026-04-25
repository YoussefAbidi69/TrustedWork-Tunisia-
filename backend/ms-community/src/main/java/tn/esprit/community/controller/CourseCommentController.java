package tn.esprit.community.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.dto.request.CourseCommentRequest;
import tn.esprit.community.dto.response.CourseCommentResponse;
import tn.esprit.community.service.CourseCommentService;

@RestController
@RequestMapping("/api/course-comments")
public class CourseCommentController {
    private final CourseCommentService commentService;

    public CourseCommentController(CourseCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<CourseCommentResponse> addComment(
            @PathVariable Long courseId, @RequestBody CourseCommentRequest commentRequest) {
        CourseCommentResponse addedComment = commentService.addComment(courseId, commentRequest);
        return new ResponseEntity<>(addedComment, HttpStatus.CREATED);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseCommentResponse>> listComments(@PathVariable Long courseId) {
        List<CourseCommentResponse> comments = commentService.listComments(courseId);
        return new ResponseEntity<>(comments, HttpStatus.OK);
    }
}
