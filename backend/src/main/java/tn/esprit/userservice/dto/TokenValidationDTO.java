package tn.esprit.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TokenValidationDTO {

    // Token valide ou non
    private boolean valid;

    // Données extraites du token — utilisées par les autres modules
    private Long userId;
    private Integer cin;
    private String email;
    private String role;
    private int trustLevel;
    private String kycStatus;
    private String accountStatus;

    // Message d'erreur si token invalide
    private String error;
}