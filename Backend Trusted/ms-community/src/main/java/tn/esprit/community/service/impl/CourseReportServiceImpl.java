package tn.esprit.community.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.request.CourseReportRequest;
import tn.esprit.community.dto.response.CourseReportResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.CourseReport;
import tn.esprit.community.entity.enums.ReportStatus;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CourseReportRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.service.CourseReportService;

@Service
public class CourseReportServiceImpl implements CourseReportService {
    private final CourseReportRepository reportRepository;
    private final CourseRepository courseRepository;

    public CourseReportServiceImpl(CourseReportRepository reportRepository, CourseRepository courseRepository) {
        this.reportRepository = reportRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public CourseReportResponse reportCourse(Long courseId, CourseReportRequest reportRequest) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new PostNotFoundException("Course not found"));

        CourseReport report = CourseReport.builder()
                .course(course)
                .reportedBy(reportRequest.getReportedBy())
                .reason(reportRequest.getReason())
                .description(reportRequest.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        return toResponse(reportRepository.save(report));
    }

    @Override
    public List<CourseReportResponse> listReportsByCourse(Long courseId) {
        return reportRepository.findByCourseIdOrderByIdDesc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CourseReportResponse updateStatus(Long reportId, ReportStatus status) {
        CourseReport report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new PostNotFoundException("Report not found"));
        report.setStatus(status);
        return toResponse(reportRepository.save(report));
    }

    private CourseReportResponse toResponse(CourseReport report) {
        return CourseReportResponse.builder()
                .id(report.getId())
                .courseId(report.getCourse() != null ? report.getCourse().getId() : null)
                .reportedBy(report.getReportedBy())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .build();
    }
}
