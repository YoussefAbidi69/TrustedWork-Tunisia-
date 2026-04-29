package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.CourseComment;

public interface CourseCommentRepository extends JpaRepository<CourseComment, Long> {
    List<CourseComment> findByCourseIdOrderByIdAsc(Long courseId);
}
