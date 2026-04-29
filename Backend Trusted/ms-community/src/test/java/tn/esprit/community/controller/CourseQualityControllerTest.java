package tn.esprit.community.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseQualityControllerTest {

    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private CourseQualityController controller;

    @BeforeEach
    void setUp() {
        controller = new CourseQualityController("http://localhost:5000");
        ReflectionTestUtils.setField(controller, "qualityClient", webClient);

        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("shouldReturnPrediction_whenAiServiceIsAvailable")
    void shouldReturnPrediction_whenAiServiceIsAvailable() {
        Map<String, Object> mockResponse = Map.of("available", true, "score", 95);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        ResponseEntity<Map<String, Object>> response = controller.predict(Map.of("title", "Java", "description", "Course"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("available")).isEqualTo(true);
        assertThat(response.getBody().get("score")).isEqualTo(95);
    }

    @Test
    @DisplayName("shouldReturnAvailableFalse_whenAiServiceFailsDuringPredict")
    void shouldReturnAvailableFalse_whenAiServiceFailsDuringPredict() {
        when(responseSpec.bodyToMono(Map.class)).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<Map<String, Object>> response = controller.predict(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("available")).isEqualTo(false);
    }

    @Test
    @DisplayName("shouldReturnPlagiarismResult_whenAiServiceIsAvailable")
    void shouldReturnPlagiarismResult_whenAiServiceIsAvailable() {
        Map<String, Object> mockResponse = Map.of("is_plagiarized", true, "max_similarity", 88.5);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        ResponseEntity<Map<String, Object>> response = controller.checkPlagiarism(Map.of("id", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("is_plagiarized")).isEqualTo(true);
        assertThat(response.getBody().get("max_similarity")).isEqualTo(88.5);
    }

    @Test
    @DisplayName("shouldReturnPlagiarizedFalse_whenAiServiceFailsDuringCheck")
    void shouldReturnPlagiarizedFalse_whenAiServiceFailsDuringCheck() {
        when(responseSpec.bodyToMono(Map.class)).thenThrow(new RuntimeException("Timeout"));

        ResponseEntity<Map<String, Object>> response = controller.checkPlagiarism(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("is_plagiarized")).isEqualTo(false);
        assertThat(response.getBody().get("error")).isEqualTo("Plagiarism service unavailable");
    }
}
