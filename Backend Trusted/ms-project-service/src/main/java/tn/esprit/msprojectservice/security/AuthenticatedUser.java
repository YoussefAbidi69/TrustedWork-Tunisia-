package tn.esprit.msprojectservice.security;

import lombok.*;

/**
 * Stocké comme principal dans le SecurityContext.
 * Accessible via Authentication.getPrincipal() dans les controllers.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticatedUser {
    private Long userId;
    private Integer cin;
    private String email;
    private String role;
}