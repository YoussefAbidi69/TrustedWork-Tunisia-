package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByIdAsc(Long postId);
    List<Comment> findByCourseIdOrderByIdAsc(Long courseId);

}
