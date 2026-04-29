package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.CourseReport;

public interface CourseReportRepository extends JpaRepository<CourseReport, Long> {
    List<CourseReport> findByCourseIdOrderByIdDesc(Long courseId);
}
