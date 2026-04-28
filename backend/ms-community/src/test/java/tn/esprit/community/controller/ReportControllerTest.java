package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.ReportRequest;
import tn.esprit.community.dto.response.ReportResponse;
import tn.esprit.community.entity.Enum.ReportStatus;
import tn.esprit.community.service.ReportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenReportPost")
    void shouldReturnCreated_whenReportPost() {
        ReportService service = mock(ReportService.class);
        when(service.reportPost(1L, ReportRequest.builder().reportedBy(1L).build()))
                .thenReturn(ReportResponse.builder().id(1L).build());
        ReportController controller = new ReportController(service);

        var response = controller.reportPost(1L, ReportRequest.builder().reportedBy(1L).build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnReports_whenListReportsByPost")
    void shouldReturnReports_whenListReportsByPost() {
        ReportService service = mock(ReportService.class);
        when(service.listReportsByPost(2L)).thenReturn(List.of(ReportResponse.builder().id(1L).build()));
        ReportController controller = new ReportController(service);

        var response = controller.listReportsByPost(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnUpdatedReport_whenUpdateStatus")
    void shouldReturnUpdatedReport_whenUpdateStatus() {
        ReportService service = mock(ReportService.class);
        when(service.updateStatus(3L, ReportStatus.REVIEWED))
                .thenReturn(ReportResponse.builder().id(3L).status(ReportStatus.REVIEWED).build());
        ReportController controller = new ReportController(service);

        var response = controller.updateStatus(3L, ReportStatus.REVIEWED);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(ReportStatus.REVIEWED);
    }
}
