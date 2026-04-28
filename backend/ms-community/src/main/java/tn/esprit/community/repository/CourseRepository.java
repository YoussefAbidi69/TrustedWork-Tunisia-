package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByCommunityIdAndPublishedTrueOrderByTitleAsc(Long communityId);

    List<Course> findByCommunityIdOrderByTitleAsc(Long communityId);
}
