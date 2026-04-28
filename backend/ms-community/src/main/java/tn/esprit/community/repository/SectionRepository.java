package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourseIdOrderByOrderIndexAsc(Long courseId);
}
