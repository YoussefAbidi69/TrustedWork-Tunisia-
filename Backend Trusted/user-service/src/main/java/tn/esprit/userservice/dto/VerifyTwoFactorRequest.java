package tn.esprit.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyTwoFactorRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    @NotBlank(message = "2FA code is required")
    @Pattern(regexp = "\\d{6}", message = "2FA code must contain exactly 6 digits")
    private String code;
}