package tn.esprit.community.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tn.esprit.community.exception.ValidationException;
import tn.esprit.community.service.impl.CourseFileStorageService;
import tn.esprit.community.service.impl.FilePostUploadService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilePostUploadServiceTest {

    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;
    @Mock private CourseFileStorageService courseFileStorageService;

    @Test
    @DisplayName("shouldThrowIllegalStateException_whenFilePostDisabled")
    void shouldThrowIllegalStateException_whenFilePostDisabled() throws Exception {
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        FilePostUploadService service = new FilePostUploadService(
                webClientBuilder,
                new ObjectMapper(),
                courseFileStorageService,
                "https://filepost.dev",
                ""
        );

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        assertThat(service.isEnabled()).isFalse();
        assertThatThrownBy(() -> service.uploadFile(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FilePost is not configured");
    }

    @Test
    @DisplayName("shouldUploadFileAndReturnUrl_whenEnabled")
    void shouldUploadFileAndReturnUrl_whenEnabled() throws Exception {
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/upload")).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("X-API-Key"), eq("key"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("{\"url\":\"https://cdn/file.pdf\"}"));

        FilePostUploadService service = new FilePostUploadService(
                webClientBuilder,
                new ObjectMapper(),
                courseFileStorageService,
                "https://filepost.dev",
                "key"
        );

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        String url = service.uploadFile(file);

        verify(courseFileStorageService).validateMediaContent(any(), eq("doc.pdf"));
        assertThat(url).isEqualTo("https://cdn/file.pdf");
    }

    @Test
    @DisplayName("shouldThrowValidationException_whenResponseEmpty")
    void shouldThrowValidationException_whenResponseEmpty() throws Exception {
        when(webClientBuilder.clientConnector(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/upload")).thenReturn(requestBodySpec);
        when(requestBodySpec.header(eq("X-API-Key"), eq("key"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(""));

        FilePostUploadService service = new FilePostUploadService(
                webClientBuilder,
                new ObjectMapper(),
                courseFileStorageService,
                "https://filepost.dev",
                "key"
        );

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.uploadFile(file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("empty response");
    }
}
