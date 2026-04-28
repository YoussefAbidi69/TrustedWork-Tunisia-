package tn.esprit.community.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseQualityControllerTest {

    @Test
    @DisplayName("shouldReturnResponse_whenPredictSucceeds")
    void shouldReturnResponse_whenPredictSucceeds() {
        CourseQualityController controller = new CourseQualityController("http://test");
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        @SuppressWarnings("rawtypes")
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ReflectionTestUtils.setField(controller, "qualityClient", webClient);
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/predict")).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(Map.of("available", true)));

        var response = controller.predict(Map.of("title", "T", "description", "D"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("available", true));
    }

    @Test
    @DisplayName("shouldReturnServiceUnavailable_whenCheckPlagiarismFails")
    void shouldReturnServiceUnavailable_whenCheckPlagiarismFails() {
        CourseQualityController controller = new CourseQualityController("http://test");
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        @SuppressWarnings("rawtypes")
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        ReflectionTestUtils.setField(controller, "qualityClient", webClient);
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/check_plagiarism")).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(Map.class))).thenThrow(new WebClientResponseException(
                503, "down", null, new byte[0], StandardCharsets.UTF_8));

        var response = controller.checkPlagiarism(Map.of("course", "data"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("is_plagiarized", false);
    }
}
