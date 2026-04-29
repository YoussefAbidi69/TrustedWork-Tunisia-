package tn.esprit.community.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.CourseVote;

public interface CourseVoteRepository extends JpaRepository<CourseVote, Long> {
    Optional<CourseVote> findByCourseIdAndUserId(Long courseId, Long userId);
}
