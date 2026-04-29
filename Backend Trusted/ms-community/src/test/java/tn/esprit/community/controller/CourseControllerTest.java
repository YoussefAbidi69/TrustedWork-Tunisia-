package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.CourseRequest;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.dto.response.CourseResponse;
import tn.esprit.community.service.CourseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseControllerTest {

    @Test
    @DisplayName("shouldReturnCreatedResponse_whenCreateCourse")
    void shouldReturnCreatedResponse_whenCreateCourse() {
        CourseService service = mock(CourseService.class);
        CourseResponse response = CourseResponse.builder().id(1L).title("T").build();
        when(service.createCourse(org.mockito.ArgumentMatchers.any(CourseRequest.class))).thenReturn(response);
        CourseController controller = new CourseController(service);

        var result = controller.createCourse(CourseRequest.builder().title("T").build());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnCourses_whenListCourses")
    void shouldReturnCourses_whenListCourses() {
        CourseService service = mock(CourseService.class);
        when(service.listCourses(null, null)).thenReturn(List.of(CourseResponse.builder().id(1L).build()));
        CourseController controller = new CourseController(service);

        var result = controller.listCourses(null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("shouldReturnDownload_whenDownloadCourse")
    void shouldReturnDownload_whenDownloadCourse() {
        CourseService service = mock(CourseService.class);
        when(service.downloadCourse(2L)).thenReturn(CourseDownloadResponse.builder().id(2L).build());
        CourseController controller = new CourseController(service);

        var result = controller.downloadCourse(2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(2L);
    }
}
