package tn.esprit.community.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.request.CourseReportRequest;
import tn.esprit.community.dto.response.CourseReportResponse;
import tn.esprit.community.entity.Course;
import tn.esprit.community.entity.CourseReport;
import tn.esprit.community.entity.enums.ReportStatus;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.repository.CourseReportRepository;
import tn.esprit.community.repository.CourseRepository;
import tn.esprit.community.service.impl.CourseReportServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseReportServiceImplTest {

    @Mock private CourseReportRepository reportRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private CourseReportServiceImpl reportService;

    @Test
    @DisplayName("shouldCreateReport_whenCourseExists")
    void shouldCreateReport_whenCourseExists() {
        Course course = Course.builder().id(3L).title("T").build();
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));

        CourseReport saved = CourseReport.builder()
                .id(11L)
                .course(course)
                .reportedBy(1L)
                .reason("Spam")
                .description("Details")
                .status(ReportStatus.PENDING)
                .build();
        when(reportRepository.save(any(CourseReport.class))).thenReturn(saved);

        CourseReportRequest request = CourseReportRequest.builder()
                .reportedBy(1L)
                .reason("Spam")
                .description("Details")
                .build();

        CourseReportResponse response = reportService.reportCourse(3L, request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("shouldThrowPostNotFoundException_whenCourseMissing")
    void shouldThrowPostNotFoundException_whenCourseMissing() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        CourseReportRequest request = CourseReportRequest.builder().reportedBy(1L).build();

        assertThatThrownBy(() -> reportService.reportCourse(99L, request))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    @DisplayName("shouldReturnReports_whenListingByCourse")
    void shouldReturnReports_whenListingByCourse() {
        Course course = Course.builder().id(2L).build();
        CourseReport report = CourseReport.builder().id(5L).course(course).status(ReportStatus.PENDING).build();
        when(reportRepository.findByCourseIdOrderByIdDesc(2L)).thenReturn(List.of(report));

        List<CourseReportResponse> responses = reportService.listReportsByCourse(2L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCourseId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("shouldUpdateStatus_whenReportExists")
    void shouldUpdateStatus_whenReportExists() {
        CourseReport report = CourseReport.builder().id(4L).status(ReportStatus.PENDING).build();
        when(reportRepository.findById(4L)).thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        CourseReportResponse response = reportService.updateStatus(4L, ReportStatus.REVIEWED);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.REVIEWED);
    }
}
