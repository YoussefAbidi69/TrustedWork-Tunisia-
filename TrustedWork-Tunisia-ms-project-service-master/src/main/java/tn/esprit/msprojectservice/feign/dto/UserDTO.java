package tn.esprit.msprojectservice.feign.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private Integer cin;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String kycStatus;
    private int trustLevel;
    private String accountStatus;
}