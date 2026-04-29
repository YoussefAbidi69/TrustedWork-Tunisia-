package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.BlockRequest;
import tn.esprit.community.dto.response.BlockResponse;
import tn.esprit.community.service.BlockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenCreateBlock")
    void shouldReturnCreated_whenCreateBlock() {
        BlockService service = mock(BlockService.class);
        when(service.createBlock(1L, BlockRequest.builder().title("T").build()))
                .thenReturn(BlockResponse.builder().id(1L).build());
        BlockController controller = new BlockController(service);

        var response = controller.createBlock(1L, BlockRequest.builder().title("T").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnBlocks_whenListBlocks")
    void shouldReturnBlocks_whenListBlocks() {
        BlockService service = mock(BlockService.class);
        when(service.listBlocks(2L)).thenReturn(List.of(BlockResponse.builder().id(1L).build()));
        BlockController controller = new BlockController(service);

        var response = controller.listBlocks(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
