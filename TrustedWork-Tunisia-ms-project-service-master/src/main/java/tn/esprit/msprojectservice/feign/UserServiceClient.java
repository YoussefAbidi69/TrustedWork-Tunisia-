package tn.esprit.msprojectservice.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tn.esprit.msprojectservice.feign.dto.TokenValidationDTO;
import tn.esprit.msprojectservice.feign.dto.UserDTO;

@Component
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user-service.base-url}")
    private String userServiceBaseUrl;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Valide le JWT auprès du User Service.
     * Retourne userId, cin, email, role.
     * Endpoint : POST /api/identity/validate-token
     */
    public TokenValidationDTO validateToken(String authHeader) {
        try {
            String url = userServiceBaseUrl + "/identity/validate-token";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<TokenValidationDTO> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, TokenValidationDTO.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("⚠️ Impossible de valider le token : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Récupère le profil public d'un utilisateur par son ID.
     * Endpoint : GET /api/identity/users/{userId}
     */
    public UserDTO getUserById(Long userId) {
        if (userId == null) return null;
        try {
            String url = userServiceBaseUrl + "/identity/users/" + userId;
            UserDTO user = restTemplate.getForObject(url, UserDTO.class);
            log.info("✅ Utilisateur récupéré : id={}, cin={}", userId,
                    user != null ? user.getCin() : "null");
            return user;
        } catch (Exception e) {
            log.warn("⚠️ Impossible de récupérer l'utilisateur id={} : {}", userId, e.getMessage());
            return null;
        }
    }
}