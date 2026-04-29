package tn.esprit.community.controller;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.dto.request.SectionRequest;
import tn.esprit.community.dto.response.SectionResponse;
import tn.esprit.community.service.SectionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectionControllerTest {

    @Test
    @DisplayName("shouldReturnCreated_whenCreateSection")
    void shouldReturnCreated_whenCreateSection() {
        SectionService service = mock(SectionService.class);
        when(service.createSection(1L, SectionRequest.builder().title("S").build()))
                .thenReturn(SectionResponse.builder().id(1L).build());
        SectionController controller = new SectionController(service);

        var response = controller.createSection(1L, SectionRequest.builder().title("S").build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("shouldReturnSections_whenListSections")
    void shouldReturnSections_whenListSections() {
        SectionService service = mock(SectionService.class);
        when(service.listSections(2L)).thenReturn(List.of(SectionResponse.builder().id(1L).build()));
        SectionController controller = new SectionController(service);

        var response = controller.listSections(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
