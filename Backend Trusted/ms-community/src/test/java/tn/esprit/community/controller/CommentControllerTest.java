package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.CommentRequest;
import tn.esprit.community.dto.response.CommentResponse;
import tn.esprit.community.service.CommentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommentControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenAddComment")
    void shouldReturnCreated_whenAddComment() {
        CommentService service = mock(CommentService.class);
        when(service.addComment(1L, CommentRequest.builder().content("C").build()))
                .thenReturn(CommentResponse.builder().id(1L).build());
        CommentController controller = new CommentController(service);

        var response = controller.addComment(1L, CommentRequest.builder().content("C").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnComments_whenListComments")
    void shouldReturnComments_whenListComments() {
        CommentService service = mock(CommentService.class);
        when(service.listComments(2L)).thenReturn(List.of(CommentResponse.builder().id(1L).build()));
        CommentController controller = new CommentController(service);

        var response = controller.listComments(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
