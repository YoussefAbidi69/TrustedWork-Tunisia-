package tn.esprit.community.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.community.service.impl.CourseFileStorageService;
import tn.esprit.community.service.impl.FilePostUploadService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadControllerTest {

    @Test
    @DisplayName("shouldUseFilePost_whenEnabled")
    void shouldUseFilePost_whenEnabled() throws Exception {
        CourseFileStorageService storageService = mock(CourseFileStorageService.class);
        FilePostUploadService filePost = mock(FilePostUploadService.class);
        when(filePost.isEnabled()).thenReturn(true);
        when(filePost.uploadFile(org.mockito.ArgumentMatchers.any())).thenReturn("https://cdn/file.pdf");
        UploadController controller = new UploadController(storageService, filePost);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        var response = controller.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("fileUrl", "https://cdn/file.pdf"));
    }

    @Test
    @DisplayName("shouldUseLocalStorage_whenFilePostDisabled")
    void shouldUseLocalStorage_whenFilePostDisabled() throws Exception {
        CourseFileStorageService storageService = mock(CourseFileStorageService.class);
        FilePostUploadService filePost = mock(FilePostUploadService.class);
        when(filePost.isEnabled()).thenReturn(false);
        when(storageService.storeFile(org.mockito.ArgumentMatchers.any())).thenReturn("http://local/file.pdf");
        UploadController controller = new UploadController(storageService, filePost);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        var response = controller.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("fileUrl", "http://local/file.pdf"));
    }

    @Test
    @DisplayName("shouldReturnServerError_whenIOExceptionOccurs")
    void shouldReturnServerError_whenIOExceptionOccurs() throws Exception {
        CourseFileStorageService storageService = mock(CourseFileStorageService.class);
        FilePostUploadService filePost = mock(FilePostUploadService.class);
        when(filePost.isEnabled()).thenReturn(false);
        when(storageService.storeFile(org.mockito.ArgumentMatchers.any())).thenThrow(new IOException("disk full"));
        UploadController controller = new UploadController(storageService, filePost);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf",
                "%PDF-1.4".getBytes(StandardCharsets.UTF_8));

        var response = controller.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Could not upload file: disk full"));
    }
}
