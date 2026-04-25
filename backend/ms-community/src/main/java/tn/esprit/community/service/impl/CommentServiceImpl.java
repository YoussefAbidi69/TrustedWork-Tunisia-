package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.request.CommentRequest;
import tn.esprit.community.dto.response.CommentResponse;
import tn.esprit.community.entity.Comment;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CommentRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.service.CommentService;

import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.exception.LearningNotFoundException;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CourseRepository courseRepository;

    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository, CourseRepository courseRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public CommentResponse addComment(Long postId, CommentRequest commentRequest) {
        if (postId == null) {
            throw new PostNotFoundException("Post ID cannot be null");
        }
        Comment comment = Comment.builder()
                .content(commentRequest.getContent())
                .userId(commentRequest.getUserId())
                .build();
        comment.setPost(postRepository.getReferenceById(postId));
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public List<CommentResponse> listComments(Long postId) {
        return commentRepository.findByPost_IdOrderByIdAsc(postId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse addCommentToCourse(Long courseId, CommentRequest commentRequest) {
        if (courseId == null) {
            throw new LearningNotFoundException("Course ID cannot be null");
        }
        Comment comment = Comment.builder()
                .content(commentRequest.getContent())
                .userId(commentRequest.getUserId())
                .build();
        comment.setCourse(courseRepository.getReferenceById(courseId));
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public List<CommentResponse> listCommentsByCourse(Long courseId) {
        return commentRepository.findByCourse_IdOrderByIdAsc(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost() != null ? comment.getPost().getId() : null)
                .userId(comment.getUserId())
                .build();
    }
}
