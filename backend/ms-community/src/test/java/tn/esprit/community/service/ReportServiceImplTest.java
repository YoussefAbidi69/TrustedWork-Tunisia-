package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.ReportRequest;
import tn.esprit.community.dto.response.ReportResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.enums.PostStatus;
import tn.esprit.community.entity.enums.ReportStatus;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Report;
import tn.esprit.community.exception.LearningNotFoundException;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.repository.ReportRepository;
import tn.esprit.community.service.impl.ReportServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportServiceImpl.
 *
 * Key business rules tested:
 *  - A post is auto-hidden (PostStatus.HIDDEN) when its reportCount reaches 3
 *  - Courses are simply recorded without auto-hiding
 *  - Report status can be updated by an admin
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportRepository reportRepository;
    @Mock private PostRepository postRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Post post(Long id, int reportCount, PostStatus status) {
        return Post.builder().id(id).title("T").reportCount(reportCount).status(status)
                .community(null).build();
    }

    private ReportRequest reportRequest() {
        return ReportRequest.builder().reportedBy(99L).reason("Spam").description("Detailed").build();
    }

    // --- reportPost ---

    @Test
    @DisplayName("shouldIncrementReportCountAndSaveReport_whenPostReported")
    void shouldIncrementReportCountAndSaveReport_whenPostReported() {
        Post post = post(1L, 0, PostStatus.PUBLISHED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Report saved = Report.builder().id(10L).post(post).reportedBy(99L)
                .reason("Spam").status(ReportStatus.PENDING).build();
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        ReportResponse response = reportService.reportPost(1L, reportRequest());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(post.getReportCount()).isEqualTo(1); // incremented in-place
        // Still PUBLISHED because count (1) < 3
        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("shouldHidePost_whenReportCountReachesThreshold")
    void shouldHidePost_whenReportCountReachesThreshold() {
        // reportCount is already 2 – one more report should trigger HIDDEN
        Post post = post(1L, 2, PostStatus.PUBLISHED);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Report saved = Report.builder().id(11L).post(post).reportedBy(99L)
                .reason("Abuse").status(ReportStatus.PENDING).build();
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        reportService.reportPost(1L, reportRequest());

        assertThat(post.getReportCount()).isEqualTo(3);
        assertThat(post.getStatus()).isEqualTo(PostStatus.HIDDEN);
    }

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenPostNotFoundOnReport")
    void shouldThrowPostNotFoundException_whenPostNotFoundOnReport() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportService.reportPost(99L, reportRequest()))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("Post not found");
    }

    // --- listReportsByPost ---

    @Test
    @DisplayName("shouldReturnReports_whenListingByPostId")
    void shouldReturnReports_whenListingByPostId() {
        Post post = post(1L, 1, PostStatus.PUBLISHED);
        Report r = Report.builder().id(1L).post(post).status(ReportStatus.PENDING).build();
        when(reportRepository.findByPostIdOrderByIdDesc(1L)).thenReturn(List.of(r));

        List<ReportResponse> result = reportService.listReportsByPost(1L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPostId()).isEqualTo(1L);
    }

    // --- reportCourse ---

    @Test
    @DisplayName("shouldSaveReportWithPendingStatus_whenCourseReported")
    void shouldSaveReportWithPendingStatus_whenCourseReported() {
        Course course = Course.builder().id(2L).title("Java 101").build();
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));

        Report saved = Report.builder().id(20L).course(course).reportedBy(99L)
                .reason("Plagiarism").status(ReportStatus.PENDING).build();
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        ReportResponse response = reportService.reportCourse(2L, reportRequest());

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getCourseId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("shouldThrowLearningNotFoundException_whenCourseNotFoundOnReport")
    void shouldThrowLearningNotFoundException_whenCourseNotFoundOnReport() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportService.reportCourse(99L, reportRequest()))
                .isInstanceOf(LearningNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    // --- updateStatus ---

    @Test
    @DisplayName("shouldUpdateReportStatus_whenAdminReviewsReport")
    void shouldUpdateReportStatus_whenAdminReviewsReport() {
        Report report = Report.builder().id(5L).status(ReportStatus.PENDING).build();
        when(reportRepository.findById(5L)).thenReturn(Optional.of(report));

        Report reviewed = Report.builder().id(5L).status(ReportStatus.REVIEWED).build();
        when(reportRepository.save(report)).thenReturn(reviewed);

        ReportResponse response = reportService.updateStatus(5L, ReportStatus.REVIEWED);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.REVIEWED);
    }

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenReportNotFoundOnStatusUpdate")
    void shouldThrowPostNotFoundException_whenReportNotFoundOnStatusUpdate() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reportService.updateStatus(99L, ReportStatus.REVIEWED))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("Report not found");
    }
}
