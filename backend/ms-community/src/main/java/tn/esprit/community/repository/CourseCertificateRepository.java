package tn.esprit.community.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.community.entity.CourseCertificate;

public interface CourseCertificateRepository extends JpaRepository<CourseCertificate, Long> {

    Optional<CourseCertificate> findByUserIdAndCourseId(Long userId, Long courseId);
}
