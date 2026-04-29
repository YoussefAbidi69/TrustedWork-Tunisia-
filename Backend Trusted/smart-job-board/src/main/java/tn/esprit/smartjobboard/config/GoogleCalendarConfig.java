package tn.esprit.smartjobboard.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class GoogleCalendarConfig {
    /**
     * Builds Google Calendar client from classpath credentials.json.
     *
     * Modes:
     * - service_account: backend/server-to-server auth (preferred for production).
     * - installed: OAuth client auth using configured refresh token.
     *
     * Note: For service accounts, share the target Google Calendar with the service account email
     * and grant "Make changes to events" permission.
     */

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "TrustedWork Smart Job Board";
    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR);
    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarConfig.class);

    private final ObjectMapper objectMapper;

    @Value("${google.calendar.oauth.refresh-token:}")
    private String oauthRefreshToken;

    @Value("${google.calendar.oauth.client-id:}")
    private String oauthClientId;

    @Value("${google.calendar.oauth.client-secret:}")
    private String oauthClientSecret;

    @Value("${google.calendar.auth-mode:auto}")
    private String authMode;

    @Value("${google.calendar.calendar-id:primary}")
    private String calendarId;

    public GoogleCalendarConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Calendar buildCalendarClient() {
        try {
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            if (shouldUseOAuthOverride()) {
                return buildOAuthCalendarClient(httpTransport, oauthClientId, oauthClientSecret, oauthRefreshToken);
            }

            CredentialFile credentialFile = readCredentialFile();

            if ("service_account".equals(credentialFile.type())) {
                log.warn("Using Service Account authentication");
                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(new ByteArrayInputStream(credentialFile.jsonBytes()))
                        .createScoped(SCOPES);
                return new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            }

            if (credentialFile.hasInstalledBlock()) {
                if (oauthRefreshToken == null || oauthRefreshToken.isBlank()) {
                    throw new IllegalStateException("OAuth credentials detected but no refresh token configured");
                }

                JsonNode installed = credentialFile.root().get("installed");
                GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
                details.setClientId(installed.path("client_id").asText());
                details.setClientSecret(installed.path("client_secret").asText());
                details.setAuthUri(installed.path("auth_uri").asText());
                details.setTokenUri(installed.path("token_uri").asText());

                GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(details);
                // OAuth installed mode in backend requires a pre-obtained refresh token.
                return buildOAuthCalendarClient(
                        httpTransport,
                        clientSecrets.getDetails().getClientId(),
                        clientSecrets.getDetails().getClientSecret(),
                        oauthRefreshToken
                );
            }

            throw new IllegalStateException(
                    "Unsupported credentials.json format. Expected either 'type=service_account' or top-level 'installed' object."
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize Google Calendar client: " + ex.getMessage(), ex);
        }
    }

    public Map<String, Object> getAuthDebugInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            if (shouldUseOAuthOverride()) {
                result.put("mode", "OAUTH");
                result.put("hasRefreshToken", oauthRefreshToken != null && !oauthRefreshToken.isBlank());
                result.put("credentialsType", "properties_override");
                result.put("calendarId", calendarId);
                result.put("status", "OK");
                result.put("message", "Ready");
                return result;
            }

            CredentialFile credentialFile = readCredentialFile();
            if ("service_account".equals(credentialFile.type())) {
                result.put("mode", "SERVICE_ACCOUNT");
                result.put("hasRefreshToken", false);
                result.put("credentialsType", "service_account");
                result.put("calendarId", calendarId);
                result.put("status", "OK");
                result.put("message", "Ready");
                return result;
            }
            if (credentialFile.hasInstalledBlock()) {
                boolean hasRefreshToken = oauthRefreshToken != null && !oauthRefreshToken.isBlank();
                result.put("mode", "OAUTH");
                result.put("hasRefreshToken", hasRefreshToken);
                result.put("credentialsType", "installed");
                result.put("calendarId", calendarId);
                if (!hasRefreshToken) {
                    result.put("status", "ERROR");
                    result.put("message", "Missing refresh token");
                } else {
                    result.put("status", "OK");
                    result.put("message", "Ready");
                }
                return result;
            }
            result.put("mode", "UNKNOWN");
            result.put("hasRefreshToken", false);
            result.put("credentialsType", "unknown");
            result.put("calendarId", calendarId);
            result.put("status", "ERROR");
            result.put("message", "Unsupported credentials format");
            return result;
        } catch (Exception ex) {
            result.put("mode", "UNKNOWN");
            result.put("hasRefreshToken", false);
            result.put("credentialsType", "unknown");
            result.put("calendarId", calendarId);
            result.put("status", "ERROR");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    public String getCalendarId() {
        return calendarId;
    }

    private boolean shouldUseOAuthOverride() {
        if (!"oauth".equalsIgnoreCase(authMode)) return false;
        return oauthClientId != null && !oauthClientId.isBlank()
                && oauthClientSecret != null && !oauthClientSecret.isBlank()
                && oauthRefreshToken != null && !oauthRefreshToken.isBlank();
    }

    private Calendar buildOAuthCalendarClient(
            NetHttpTransport httpTransport,
            String clientId,
            String clientSecret,
            String refreshToken
    ) throws Exception {
        log.warn("Using OAuth authentication with refresh token");
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);
        if (!credential.refreshToken()) {
            throw new IllegalStateException(
                    "OAuth refresh token is invalid/expired. Re-authorize and update google.calendar.oauth.refresh-token."
            );
        }

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private CredentialFile readCredentialFile() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("credentials.json");
        if (stream == null) {
            throw new IllegalStateException("credentials.json not found on classpath.");
        }
        byte[] bytes = stream.readAllBytes();
        JsonNode root = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
        String type = root.path("type").asText("");
        return new CredentialFile(type, root, bytes);
    }

    private record CredentialFile(String type, JsonNode root, byte[] jsonBytes) {
        boolean hasInstalledBlock() {
            return root.has("installed");
        }
    }
}
