package tn.esprit.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Email
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String twoFactorCode;

    // Adresse IP du client — utilisée pour le Session Risk Score
    // Envoyée par le frontend ou extraite côté contrôleur
    private String ipAddress;
}