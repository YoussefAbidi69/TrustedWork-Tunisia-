package tn.esprit.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PublicUserDTO {

    // Identifiants
    private Long id;
    private Integer cin;

    // Infos de base — nécessaires pour les modules 02, 03, 05
    private String firstName;
    private String lastName;
    private String email;

    // Rôle — utilisé par tous les modules pour le contrôle d'accès
    private String role;

    // KYC — utilisé par Module 05 (Contract Health Score)
    private String kycStatus;

    // Trust Level — utilisé par Module 03 (Match Score) et Module 05
    private int trustLevel;

    // Compte actif — vérification rapide avant toute interaction
    private String accountStatus;
}