package tn.esprit.community.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.community.exception.ValidationException;
import tn.esprit.community.service.impl.CourseFileStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("shouldReturnNull_whenStoredNameIsBlankOrNull")
    void shouldReturnNull_whenStoredNameIsBlankOrNull() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://localhost");

        assertThat(service.resolveStoredFile(null)).isNull();
        assertThat(service.resolveStoredFile(" ")).isNull();
    }

    @Test
    @DisplayName("shouldReturnNull_whenStoredNameHasTraversalOrSeparator")
    void shouldReturnNull_whenStoredNameHasTraversalOrSeparator() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://localhost");

        assertThat(service.resolveStoredFile("../secret.pdf")).isNull();
        assertThat(service.resolveStoredFile("dir/secret.pdf")).isNull();
    }

    @Test
    @DisplayName("shouldReturnNull_whenStoredNameHasInvalidExtension")
    void shouldReturnNull_whenStoredNameHasInvalidExtension() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://localhost");

        assertThat(service.resolveStoredFile("file.exe")).isNull();
    }

    @Test
    @DisplayName("shouldResolvePath_whenStoredNameIsValid")
    void shouldResolvePath_whenStoredNameIsValid() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://localhost");
        String legacy = "course.pdf";
        String uuidName = UUID.randomUUID() + ".png";

        assertThat(service.resolveStoredFile(legacy)).isNotNull();
        assertThat(service.resolveStoredFile(uuidName)).isNotNull();
    }

    @Test
    @DisplayName("shouldThrowValidationException_whenFileTypeInvalid")
    void shouldThrowValidationException_whenFileTypeInvalid() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://localhost");

        assertThatThrownBy(() -> service.validateMediaContent("data".getBytes(StandardCharsets.UTF_8), "file.exe"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Only PDF");
    }

    @Test
    @DisplayName("shouldThrowValidationException_whenPdfHeaderInvalid")
    void shouldThrowValidationException_whenPdfHeaderInvalid() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://localhost");

        assertThatThrownBy(() -> service.validateMediaContent("NOTPDF".getBytes(StandardCharsets.UTF_8), "file.pdf"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("valid PDF");
    }

    @Test
    @DisplayName("shouldStoreFileAndReturnUrl_whenFileIsValid")
    void shouldStoreFileAndReturnUrl_whenFileIsValid() throws IOException {
        CourseFileStorageService service = new CourseFileStorageService(tempDir.toString(), "http://example.com");
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdf);

        String url = service.storeFile(file);

        assertThat(url).startsWith("http://example.com/api/course-files/");
        String storedName = url.substring(url.lastIndexOf('/') + 1);
        Path storedPath = tempDir.resolve(storedName);
        assertThat(Files.exists(storedPath)).isTrue();
    }
}
