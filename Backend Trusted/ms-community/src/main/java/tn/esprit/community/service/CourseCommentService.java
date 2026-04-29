package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.CourseCommentRequest;
import tn.esprit.community.dto.response.CourseCommentResponse;

public interface CourseCommentService {
    CourseCommentResponse addComment(Long courseId, CourseCommentRequest commentRequest);

    List<CourseCommentResponse> listComments(Long courseId);
    void deleteComment(Long id);
}
