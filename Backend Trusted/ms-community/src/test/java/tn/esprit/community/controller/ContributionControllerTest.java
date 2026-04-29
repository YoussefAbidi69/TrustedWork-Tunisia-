package tn.esprit.community.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tn.esprit.community.entity.Contribution;
import tn.esprit.community.service.ContributionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContributionControllerTest {

    @Test
    @DisplayName("shouldReturnDefaultContribution_whenServiceReturnsNull")
    void shouldReturnDefaultContribution_whenServiceReturnsNull() {
        ContributionService service = mock(ContributionService.class);
        when(service.getContribution(8L)).thenReturn(null);
        ContributionController controller = new ContributionController(service);

        var response = controller.getContribution(8L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(8L);
        assertThat(response.getBody().getScore()).isEqualTo(0);
    }
}
