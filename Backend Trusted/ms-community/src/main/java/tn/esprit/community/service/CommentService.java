package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.CommentRequest;
import tn.esprit.community.dto.response.CommentResponse;

public interface CommentService {
    CommentResponse addComment(Long postId, CommentRequest commentRequest);

    List<CommentResponse> listComments(Long postId);

    CommentResponse addCommentToCourse(Long courseId, CommentRequest commentRequest);

    List<CommentResponse> listCommentsByCourse(Long courseId);
    void deleteComment(Long id);
}
