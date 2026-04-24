package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.UserDTO;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.User;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .cin(user.getCin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .photo(user.getPhoto())
                .headline(user.getHeadline())
                .location(user.getLocation())
                .bio(user.getBio())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .accountStatus(user.getAccountStatus() != null
                        ? user.getAccountStatus().name() : null)
                .kycStatus(user.getKycStatus() != null
                        ? user.getKycStatus().name() : null)
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .trustLevel(user.getTrustLevel())
                .livenessPassed(user.isLivenessPassed())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public void initNewUser(User user) {
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setKycStatus(KycStatus.PENDING);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setTwoFactorEnabled(false);
        user.setTrustLevel(1);
        user.setLivenessPassed(false);
        user.setFailedAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
    }
}