package tn.esprit.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload envoyé par la page "Compléter mon profil" après un premier login Google.
 * Permet de renseigner les champs obligatoires manquants (CIN, téléphone, rôle).
 */
@Getter
@Setter
public class CompleteGoogleProfileRequest {

    @NotNull(message = "CIN is required")
    @Pattern(regexp = "^\\d{8}$", message = "CIN must be exactly 8 digits")
    private String cin;

    @Size(min = 8, max = 15, message = "Phone number must be between 8 and 15 digits")
    @Pattern(regexp = "^\\d{8,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @Pattern(regexp = "^(FREELANCER|CLIENT)$", message = "Role must be FREELANCER or CLIENT")
    private String role;
}
