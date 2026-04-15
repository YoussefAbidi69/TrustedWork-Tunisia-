package tn.esprit.community.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tn.esprit.community.entity.Contribution;
import tn.esprit.community.entity.Post;
import tn.esprit.community.entity.Enum.PostStatus;
import tn.esprit.community.entity.Enum.PostType;
import tn.esprit.community.exception.DownloadNotAllowedException;
import tn.esprit.community.exception.PostNotFoundException;
import tn.esprit.community.exception.UpstreamCourseFileException;
import tn.esprit.community.repository.ContributionRepository;
import tn.esprit.community.repository.PostRepository;
import tn.esprit.community.service.DownloadService;

@Service
public class DownloadServiceImpl implements DownloadService {
    private final PostRepository postRepository;
    private final ContributionRepository contributionRepository;
    private final String externalApiBaseUrl;

    public DownloadServiceImpl(
            PostRepository postRepository,
            ContributionRepository contributionRepository,
            @Value("${app.external-api-base-url:http://localhost:8084}") String externalApiBaseUrl) {
        this.postRepository = postRepository;
        this.contributionRepository = contributionRepository;
        this.externalApiBaseUrl = externalApiBaseUrl.replaceAll("/$", "");
    }

    @Override
    public StreamingResponseBody canDownload(Long userId, Long postId) {
        ensureCanDownload(userId, postId);
        return outputStream -> outputStream.flush();
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadCourse(Long userId, Long postId) {
        ensureCanDownload(userId, postId);
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Post not found"));
        String fileUrl = resolveHttpFileUrl(post.getFileUrl().trim());
        try {
            preflightRemoteUrl(fileUrl);
        } catch (UpstreamCourseFileException e) {
            throw e;
        } catch (IOException e) {
            throw new UpstreamCourseFileException(upstreamHint(fileUrl, e), e);
        }

        StreamingResponseBody body = outputStream -> {
            HttpURLConnection conn = null;
            try {
                conn = openHttpGet(fileUrl);
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new UpstreamCourseFileException(buildUpstreamMessage(code, fileUrl));
                }
                try (InputStream in = conn.getInputStream()) {
                    in.transferTo(outputStream);
                }
            } catch (UpstreamCourseFileException e) {
                throw e;
            } catch (Exception e) {
                throw new UpstreamCourseFileException(upstreamHint(fileUrl, e), e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        };
        String filename = "course-" + postId;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    /** Quick check so we return 502 before committing a 200 OK stream. */
    private void preflightRemoteUrl(String fileUrl) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openHttpHead(fileUrl);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return;
            }
            // Many CDNs return 403/401 to HEAD even when GET (used for download) works.
            if (code == 405
                    || code == HttpURLConnection.HTTP_NOT_IMPLEMENTED
                    || code == HttpURLConnection.HTTP_FORBIDDEN
                    || code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return;
            }
            throw new UpstreamCourseFileException(buildUpstreamMessage(code, fileUrl));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static HttpURLConnection openHttpHead(String fileUrl) throws IOException {
        HttpURLConnection conn = openConnection(fileUrl);
        conn.setRequestMethod("HEAD");
        return conn;
    }

    private static HttpURLConnection openHttpGet(String fileUrl) throws IOException {
        HttpURLConnection conn = openConnection(fileUrl);
        conn.setRequestMethod("GET");
        return conn;
    }

    private static HttpURLConnection openConnection(String fileUrl) throws IOException {
        URI uri = URI.create(fileUrl);
        String scheme = uri.getScheme();
        if (scheme == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new DownloadNotAllowedException("Only http(s) file URLs are supported");
        }
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);
        return conn;
    }

    private static String buildUpstreamMessage(int httpCode, String fileUrl) {
        String hint = "";
        if (httpCode == HttpURLConnection.HTTP_NOT_FOUND) {
            hint = " Use the exact file name and extension in the URL (for example TrustedWork_CDC.pdf).";
        }
        return "The file URL returned HTTP " + httpCode + ": " + fileUrl + "." + hint;
    }

    private static String upstreamHint(String fileUrl, Throwable e) {
        String base = "Could not fetch the PDF from: " + fileUrl + ". " + e.getMessage();
        Throwable c = e;
        while (c != null) {
            if (c instanceof ConnectException) {
                return base
                        + " If ms-community runs in Docker, replace localhost in the URL with host.docker.internal"
                        + " (example: http://host.docker.internal:8000/your-file.pdf).";
            }
            if (c instanceof UnknownHostException) {
                return base + " Check the hostname in fileUrl.";
            }
            c = c.getCause();
        }
        return base;
    }

    /**
     * Legacy posts may store only a path or bare filename (e.g. {@code file2.pdf}). Downloads require an http(s) URL.
     */
    private String resolveHttpFileUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) {
            return t;
        }
        if (t.startsWith("/")) {
            return externalApiBaseUrl + t;
        }
        if (t.matches("(?i)^[a-zA-Z0-9._-]+\\.pdf$")) {
            return externalApiBaseUrl + "/api/course-files/" + t;
        }
        return t;
    }

    private void ensureCanDownload(Long userId, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Post not found"));
        if (post.getType() != PostType.COURSE || post.getStatus() != PostStatus.PUBLISHED) {
            throw new DownloadNotAllowedException("Only published course posts can be downloaded");
        }
        if (post.getFileUrl() == null || post.getFileUrl().isBlank()) {
            throw new DownloadNotAllowedException("This post has no file URL");
        }
        if (userId != null && userId.equals(post.getCreatedBy())) {
            return;
        }
        int shares = contributionRepository
                .findByUserId(userId)
                .map(Contribution::getSharedCourseCount)
                .orElse(0);
        if (shares < 1) {
            throw new DownloadNotAllowedException("Share at least one course to unlock downloads");
        }
    }
}
