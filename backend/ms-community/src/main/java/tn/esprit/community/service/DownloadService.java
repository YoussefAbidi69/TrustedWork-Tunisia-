package tn.esprit.community.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface DownloadService {
    StreamingResponseBody canDownload(Long userId, Long postId);
    ResponseEntity<StreamingResponseBody> downloadCourse(Long userId, Long postId);
}
