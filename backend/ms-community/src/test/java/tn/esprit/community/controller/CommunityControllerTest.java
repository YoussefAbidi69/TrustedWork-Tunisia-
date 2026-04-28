package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.CommunityRequest;
import tn.esprit.community.dto.response.CommunityResponse;
import tn.esprit.community.service.CommunityService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenCreateCommunity")
    void shouldReturnCreated_whenCreateCommunity() {
        CommunityService service = mock(CommunityService.class);
        when(service.createCommunity(CommunityRequest.builder().name("N").build()))
                .thenReturn(CommunityResponse.builder().id(1L).build());
        CommunityController controller = new CommunityController(service);

        var response = controller.createCommunity(CommunityRequest.builder().name("N").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnCommunities_whenListCommunities")
    void shouldReturnCommunities_whenListCommunities() {
        CommunityService service = mock(CommunityService.class);
        when(service.listCommunities()).thenReturn(List.of(CommunityResponse.builder().id(1L).build()));
        CommunityController controller = new CommunityController(service);

        var response = controller.listCommunities();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
