package tn.esprit.smartjobboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Lightweight projection of user-service {@code UserDTO} JSON for REST calls (no JPA entity).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserReferenceDto {
    private Long id;
    private Integer cin;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String accountStatus;
    private String kycStatus;
}
