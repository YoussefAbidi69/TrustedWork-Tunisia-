package tn.esprit.community.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.service.DownloadService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadControllerTest {

    @Test
    @DisplayName("shouldReturnDownload_whenCourseExists")
    void shouldReturnDownload_whenCourseExists() {
        DownloadService service = mock(DownloadService.class);
        when(service.downloadCourse(3L)).thenReturn(CourseDownloadResponse.builder().id(3L).build());
        DownloadController controller = new DownloadController(service);

        var response = controller.download(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(3L);
    }
}
