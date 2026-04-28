package tn.esprit.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.community.dto.response.CourseDownloadResponse;
import tn.esprit.community.service.impl.DownloadServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownloadServiceImplTest {

    @Mock private CourseService courseService;

    @InjectMocks
    private DownloadServiceImpl downloadService;

    @Test
    @DisplayName("shouldReturnDownloadResponse_whenCourseExists")
    void shouldReturnDownloadResponse_whenCourseExists() {
        CourseDownloadResponse response = CourseDownloadResponse.builder().id(1L).title("T").build();
        when(courseService.downloadCourse(1L)).thenReturn(response);

        CourseDownloadResponse result = downloadService.downloadCourse(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("T");
    }
}
