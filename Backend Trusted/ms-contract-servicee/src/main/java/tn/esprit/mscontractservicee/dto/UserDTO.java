package tn.esprit.mscontractservicee.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {

    private Long id;
    private Integer cin;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String photo;
    private String role;
    private String accountStatus;
    private String kycStatus;
    private boolean twoFactorEnabled;
    private LocalDateTime createdAt;
}
