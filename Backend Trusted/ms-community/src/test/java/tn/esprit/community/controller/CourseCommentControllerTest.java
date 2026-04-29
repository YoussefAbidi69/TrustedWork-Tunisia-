package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.CourseCommentRequest;
import tn.esprit.community.dto.response.CourseCommentResponse;
import tn.esprit.community.service.CourseCommentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseCommentControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenAddComment")
    void shouldReturnCreated_whenAddComment() {
        CourseCommentService service = mock(CourseCommentService.class);
        when(service.addComment(1L, CourseCommentRequest.builder().content("C").build()))
                .thenReturn(CourseCommentResponse.builder().id(1L).build());
        CourseCommentController controller = new CourseCommentController(service);

        var response = controller.addComment(1L, CourseCommentRequest.builder().content("C").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnComments_whenListComments")
    void shouldReturnComments_whenListComments() {
        CourseCommentService service = mock(CourseCommentService.class);
        when(service.listComments(2L)).thenReturn(List.of(CourseCommentResponse.builder().id(1L).build()));
        CourseCommentController controller = new CourseCommentController(service);

        var response = controller.listComments(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
