package tn.esprit.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer cin;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @Column(name = "photo")
    private String photo;

    // 🔥 NOUVEAU
    @Column(length = 100)
    private String headline;

    @Column(length = 100)
    private String location;

    @Column(length = 1000)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    private String secret2FA;

    @Column(nullable = false)
    private int trustLevel = 1;

    @Column(nullable = false)
    private boolean livenessPassed = false;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }
        if (this.kycStatus == null) {
            this.kycStatus = KycStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}