package tn.esprit.community.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
	List<Report> findByPostIdOrderByIdDesc(Long postId);
}
