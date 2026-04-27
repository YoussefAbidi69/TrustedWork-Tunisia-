package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tn.esprit.community.dto.request.CourseCommentRequest;
import tn.esprit.community.dto.response.CourseCommentResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.CourseComment;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CourseCommentRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.service.CourseCommentService;

@Service
public class CourseCommentServiceImpl implements CourseCommentService {
    private final CourseCommentRepository commentRepository;
    private final CourseRepository courseRepository;

    public CourseCommentServiceImpl(CourseCommentRepository commentRepository, CourseRepository courseRepository) {
        this.commentRepository = commentRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public CourseCommentResponse addComment(Long courseId, CourseCommentRequest commentRequest) {
        if (courseId == null) {
            throw new PostNotFoundException("Course ID cannot be null");
        }
        CourseComment comment = CourseComment.builder()
                .content(commentRequest.getContent())
                .userId(commentRequest.getUserId())
                .build();
        comment.setCourse(courseRepository.getReferenceById(courseId));
        return toResponse(commentRepository.save(comment));
    }

    @Override
    public List<CourseCommentResponse> listComments(Long courseId) {
        return commentRepository.findByCourseIdOrderByIdAsc(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }

    private CourseCommentResponse toResponse(CourseComment comment) {
        return CourseCommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .courseId(comment.getCourse() != null ? comment.getCourse().getId() : null)
                .userId(comment.getUserId())
                .build();
    }
}
