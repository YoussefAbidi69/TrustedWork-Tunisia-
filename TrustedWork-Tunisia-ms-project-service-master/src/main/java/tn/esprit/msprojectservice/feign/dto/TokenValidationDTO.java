package tn.esprit.msprojectservice.feign.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationDTO {
    private boolean valid;
    private Long userId;
    private Integer cin;
    private String email;
    private String role;
    private int trustLevel;
    private String kycStatus;
    private String accountStatus;
    private String error;
}