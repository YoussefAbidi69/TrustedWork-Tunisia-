package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.PostRequest;
import tn.esprit.community.dto.response.PostResponse;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.service.PostService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenCreatePost")
    void shouldReturnCreated_whenCreatePost() {
        PostService service = mock(PostService.class);
        when(service.createPost(PostRequest.builder().title("T").build()))
                .thenReturn(PostResponse.builder().id(1L).build());
        PostController controller = new PostController(service);

        var response = controller.createPost(PostRequest.builder().title("T").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnPosts_whenListPosts")
    void shouldReturnPosts_whenListPosts() {
        PostService service = mock(PostService.class);
        when(service.listPosts(null, PostStatus.PUBLISHED, null)).thenReturn(List.of(PostResponse.builder().id(1L).build()));
        PostController controller = new PostController(service);

        var response = controller.listPosts(null, PostStatus.PUBLISHED, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
