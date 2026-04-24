package tn.esprit.userservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserDTO {

    private Long id;
    private Integer cin;

    private String firstName;
    private String lastName;
    private String email;

    private String phone;
    private String photo;

    private String headline;
    private String location;
    private String bio;

    private String role;
    private String accountStatus;
    private String kycStatus;

    private boolean twoFactorEnabled;

    private int trustLevel;
    private boolean livenessPassed;

    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}