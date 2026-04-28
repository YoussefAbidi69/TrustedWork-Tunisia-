package tn.esprit.community.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.CourseVoteRequest;
import tn.esprit.community.dto.response.CourseVoteResponse;
import tn.esprit.community.entity.enums.VoteType;
import tn.esprit.community.service.CourseVoteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseVoteControllerTest {

    @Test
    @DisplayName("shouldReturnOk_whenVoteCreated")
    void shouldReturnOk_whenVoteCreated() {
        CourseVoteService service = mock(CourseVoteService.class);
        when(service.vote(1L, CourseVoteRequest.builder().userId(2L).type(VoteType.DOWN).build()))
                .thenReturn(CourseVoteResponse.builder().id(1L).build());
        CourseVoteController controller = new CourseVoteController(service);

        var response = controller.vote(1L, CourseVoteRequest.builder().userId(2L).type(VoteType.DOWN).build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }
}
