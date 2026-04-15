package tn.esprit.community.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.lms.CertificateDTO;
import tn.esprit.community.dto.lms.CertificateRequestDTO;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.CourseCertificate;
import tn.esprit.community.entity.Lesson;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.exception.ValidationException;
import tn.esprit.community.repository.CourseCertificateRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.LessonProgressRepository;
import tn.esprit.community.repository.LessonRepository;
import tn.esprit.community.repository.SectionRepository;
import tn.esprit.community.service.LearningCertificateService;

@Service
public class LearningCertificateServiceImpl implements LearningCertificateService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseCertificateRepository courseCertificateRepository;

    public LearningCertificateServiceImpl(
            CourseRepository courseRepository,
            SectionRepository sectionRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            CourseCertificateRepository courseCertificateRepository) {
        this.courseRepository = courseRepository;
        this.sectionRepository = sectionRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.courseCertificateRepository = courseCertificateRepository;
    }

    @Override
    @Transactional
    public CertificateDTO issueCertificate(CertificateRequestDTO request) {
        if (request.getUserId() == null || request.getCourseId() == null) {
            throw new LearningNotFoundException("userId and courseId are required");
        }
        Course course = courseRepository
                .findById(request.getCourseId())
                .orElseThrow(() -> new LearningNotFoundException("Course not found"));

        var existing = courseCertificateRepository.findByUserIdAndCourseId(request.getUserId(), request.getCourseId());
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        List<Lesson> allLessons = new ArrayList<>();
        var sections = sectionRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId());
        for (var sec : sections) {
            allLessons.addAll(lessonRepository.findBySection_IdOrderByOrderIndexAsc(sec.getId()));
        }
        if (allLessons.isEmpty()) {
            throw new ValidationException("Course has no lessons");
        }
        long completed = allLessons.stream()
                .filter(
                        l -> lessonProgressRepository
                                .findByUserIdAndLessonId(request.getUserId(), l.getId())
                                .map(p -> p.isCompleted())
                                .orElse(false))
                .count();
        if (completed < allLessons.size()) {
            throw new ValidationException("Complete every lesson before requesting a certificate");
        }

        CourseCertificate cert = CourseCertificate.builder()
                .userId(request.getUserId())
                .courseId(request.getCourseId())
                .issuedAt(Instant.now())
                .build();
        return toDto(courseCertificateRepository.save(cert));
    }

    private CertificateDTO toDto(CourseCertificate c) {
        return CertificateDTO.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .courseId(c.getCourseId())
                .issuedAt(c.getIssuedAt())
                .build();
    }
}
