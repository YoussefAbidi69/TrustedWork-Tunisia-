package tn.esprit.community.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.community.dto.request.ReportRequest;
import tn.esprit.community.dto.response.ReportResponse;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Report;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.ReportStatus;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.ReportRepository;
import tn.esprit.community.service.ReportService;

import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.entity.Course;
import tn.esprit.community.exception.LearningNotFoundException;

@Service
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CourseRepository courseRepository;

    public ReportServiceImpl(ReportRepository reportRepository, PostRepository postRepository, CourseRepository courseRepository) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public ReportResponse reportPost(Long postId, ReportRequest reportRequest) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Post not found"));

        Report report = Report.builder()
                .post(post)
                .reportedBy(reportRequest.getReportedBy())
                .reason(reportRequest.getReason())
                .description(reportRequest.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        post.setReportCount(post.getReportCount() + 1);
        if (post.getReportCount() >= 3) {
            post.setStatus(PostStatus.HIDDEN);
        }
        postRepository.save(post);

        return toResponse(reportRepository.save(report));
    }

    @Override
    public List<ReportResponse> listReportsByPost(Long postId) {
        return reportRepository.findByPost_IdOrderByIdDesc(postId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReportResponse reportCourse(Long courseId, ReportRequest reportRequest) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new LearningNotFoundException("Course not found"));

        Report report = Report.builder()
                .course(course)
                .reportedBy(reportRequest.getReportedBy())
                .reason(reportRequest.getReason())
                .description(reportRequest.getDescription())
                .status(ReportStatus.PENDING)
                .build();

        // Course doesn't auto-hide like posts yet, we just record the report for admins
        return toResponse(reportRepository.save(report));
    }

    @Override
    public List<ReportResponse> listReportsByCourse(Long courseId) {
        // Since we don't have findByCourse_IdOrderByIdDesc yet, let's use findAll and filter, or just use it assuming we'll add it
        return reportRepository.findAll().stream()
                .filter(r -> r.getCourse() != null && r.getCourse().getId().equals(courseId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReportResponse updateStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository
                .findById(reportId)
                .orElseThrow(() -> new PostNotFoundException("Report not found"));
        report.setStatus(status);
        return toResponse(reportRepository.save(report));
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .postId(report.getPost() != null ? report.getPost().getId() : null)
                .courseId(report.getCourse() != null ? report.getCourse().getId() : null)
                .reportedBy(report.getReportedBy())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .build();
    }
}
