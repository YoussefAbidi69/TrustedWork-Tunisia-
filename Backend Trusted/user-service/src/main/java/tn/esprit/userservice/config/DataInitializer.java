package tn.esprit.userservice.config;

/*import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initAdmin() {
        return args -> {

            String adminEmail = "admin@trustedwork.com";

            if (userRepository.existsByEmail(adminEmail)) {
                log.info("Admin already exists");
                return;
            }

            User admin = new User();
            admin.setCin(99999999); // CIN admin

            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setPhone("00000000");

            admin.setRole(Role.ADMIN);

            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setKycStatus(KycStatus.APPROVED);
            admin.setEnabled(true);
            admin.setAccountNonLocked(true);
            admin.setTwoFactorEnabled(false);

            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());

            userRepository.save(admin);

            log.info("Default admin created: {}", adminEmail);
        };
    }
}
*/
