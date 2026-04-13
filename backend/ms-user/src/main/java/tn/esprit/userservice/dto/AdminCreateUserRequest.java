package tn.esprit.userservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotNull(message = "CIN is required")
    @Min(value = 10000000, message = "CIN must be 8 digits")
    @Max(value = 99999999, message = "CIN must be 8 digits")
    private Integer cin;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String phoneNumber;

    private String photo;

    @NotBlank(message = "Role is required")
    private String role;
}