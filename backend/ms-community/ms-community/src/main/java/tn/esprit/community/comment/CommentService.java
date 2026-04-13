package tn.esprit.community.comment;

import java.util.List;
import tn.esprit.community.dto.CommentDTO;

public interface CommentService {
    CommentDTO addComment(Long postId, CommentDTO commentDTO);
    List<CommentDTO> listComments(Long postId);
}
