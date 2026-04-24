package tn.esprit.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload reçu du frontend après que l'utilisateur a signé avec Google.
 * Le frontend envoie le credential (JWT ID Token) retourné par Google Identity Services.
 */
@Getter
@Setter
public class GoogleOAuthRequest {

    @NotBlank(message = "Google credential (ID token) is required")
    private String credential;
}
