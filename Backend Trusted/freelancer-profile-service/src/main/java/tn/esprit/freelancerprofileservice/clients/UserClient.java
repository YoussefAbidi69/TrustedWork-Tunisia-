package tn.esprit.freelancerprofileservice.clients;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "http://localhost:8081/api/users/";
    // Endpoint identity — expose phone
    private static final String IDENTITY_URL = "http://localhost:8081/api/identity/users/";

    public PublicUserResponse getPublicUser(Long userId) {
        try {
            // Utiliser l'endpoint identity qui expose le phone
            String url = IDENTITY_URL + userId;
            log.debug(">>> USER CLIENT IDENTITY URL = {}", url);

            PublicUserResponse response = restTemplate.getForObject(url, PublicUserResponse.class);

            if (response == null) {
                log.warn(">>> USER CLIENT - response is null, fallback legacy");
                // Fallback vers l'ancien endpoint
                url = BASE_URL + userId + "/public";
                response = restTemplate.getForObject(url, PublicUserResponse.class);
            }

            return response;

        } catch (Exception e) {
            log.error(">>> USER CLIENT ERROR - {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserFullName(Long userId) {
        PublicUserResponse response = getPublicUser(userId);
        if (response == null) return "Unknown User";
        String firstName = response.getFirstName() != null ? response.getFirstName().trim() : "";
        String lastName  = response.getLastName()  != null ? response.getLastName().trim()  : "";
        String fullName  = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? "Unknown User" : fullName;
    }

    public String getUserEmail(Long userId) {
        PublicUserResponse response = getPublicUser(userId);
        if (response == null || response.getEmail() == null) return null;
        String email = response.getEmail().trim();
        return email.isBlank() ? null : email;
    }

    /**
     * Suspend le compte utilisateur via le user-service (appelé par l'IA).
     * Endpoint interne POST /api/users/{userId}/auto-suspend.
     */
    public void autoSuspendUser(Long userId, String reason) {
        try {
            String url = BASE_URL + userId + "/auto-suspend";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(
                    Map.of("reason", reason), headers);
            restTemplate.postForObject(url, request, Void.class);
            log.info(">>> USER CLIENT AUTO-SUSPEND — userId={} suspendu par IA", userId);
        } catch (Exception e) {
            log.error(">>> USER CLIENT AUTO-SUSPEND ERROR — {}", e.getMessage(), e);
        }
    }

    /**
     * Récupère le numéro de téléphone d'un utilisateur pour l'envoi de SMS.
     */
    public String getUserPhone(Long userId) {
        PublicUserResponse response = getPublicUser(userId);
        if (response == null || response.getPhone() == null) return null;
        String phone = response.getPhone().trim();
        return phone.isBlank() ? null : phone;
    }

    @Data
    public static class PublicUserResponse {
        private Long id;
        private Integer cin;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String role;
        private String kycStatus;
        private int trustLevel;
        private String accountStatus;
    }
}