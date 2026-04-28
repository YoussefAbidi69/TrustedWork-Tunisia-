package tn.esprit.community.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.VoteRequest;
import tn.esprit.community.dto.response.VoteResponse;
import tn.esprit.community.entity.Enum.VoteType;
import tn.esprit.community.service.VoteService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoteControllerTest {

    @Test
    @DisplayName("shouldReturnOk_whenVoteCreated")
    void shouldReturnOk_whenVoteCreated() {
        VoteService service = mock(VoteService.class);
        when(service.vote(1L, VoteRequest.builder().userId(2L).type(VoteType.UP).build()))
                .thenReturn(VoteResponse.builder().id(1L).build());
        VoteController controller = new VoteController(service);

        var response = controller.vote(1L, VoteRequest.builder().userId(2L).type(VoteType.UP).build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }
}
