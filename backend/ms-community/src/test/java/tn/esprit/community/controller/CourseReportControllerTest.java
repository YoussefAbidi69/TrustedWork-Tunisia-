package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.CourseReportRequest;
import tn.esprit.community.dto.response.CourseReportResponse;
import tn.esprit.community.entity.enums.ReportStatus;
import tn.esprit.community.service.CourseReportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseReportControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenReportCourse")
    void shouldReturnCreated_whenReportCourse() {
        CourseReportService service = mock(CourseReportService.class);
        when(service.reportCourse(1L, CourseReportRequest.builder().reportedBy(1L).build()))
                .thenReturn(CourseReportResponse.builder().id(1L).build());
        CourseReportController controller = new CourseReportController(service);

        var response = controller.reportCourse(1L, CourseReportRequest.builder().reportedBy(1L).build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnReports_whenListReportsByCourse")
    void shouldReturnReports_whenListReportsByCourse() {
        CourseReportService service = mock(CourseReportService.class);
        when(service.listReportsByCourse(2L)).thenReturn(List.of(CourseReportResponse.builder().id(1L).build()));
        CourseReportController controller = new CourseReportController(service);

        var response = controller.listReportsByCourse(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnUpdatedReport_whenUpdateStatus")
    void shouldReturnUpdatedReport_whenUpdateStatus() {
        CourseReportService service = mock(CourseReportService.class);
        when(service.updateStatus(3L, ReportStatus.REVIEWED))
                .thenReturn(CourseReportResponse.builder().id(3L).status(ReportStatus.REVIEWED).build());
        CourseReportController controller = new CourseReportController(service);

        var response = controller.updateStatus(3L, ReportStatus.REVIEWED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(ReportStatus.REVIEWED);
    }
}
