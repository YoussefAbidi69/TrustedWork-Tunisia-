package tn.esprit.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotNull(message = "CIN is required")
    @Min(value = 10000000, message = "CIN must be 8 digits")
    @Max(value = 99999999, message = "CIN must be 8 digits")
    private Integer cin;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phoneNumber;

    private String photo;

    @NotBlank(message = "Role is required")
    private String role; // CLIENT, FREELANCER
}