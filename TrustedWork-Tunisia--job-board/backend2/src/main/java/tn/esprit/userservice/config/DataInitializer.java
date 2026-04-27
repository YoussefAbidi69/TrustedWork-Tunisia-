package tn.esprit.userservice.config;

import lombok.RequiredArgsConstructor;
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
    public CommandLineRunner initDemoUsers() {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Users already exist. Skipping demo data loader.");
                return;
            }

            log.info("Seeding demo users...");

            // 1. Admin (ID: 1)
            createUser("admin@trustedwork.com", "Admin", "System", "Admin123!", Role.ADMIN, 10000001);
            
            // 2. Client (ID: 2)
            createUser("client@trustedwork.tn", "Enterprise", "Client", "Client123!", Role.CLIENT, 10000002);

            // 3. Freelancer 1 (ID: 3)
            createUser("freelancer.demo1@trustedwork.tn", "Backend", "Pro", "Free123!", Role.FREELANCER, 10000003);

            // 4. Freelancer 2 (ID: 4)
            createUser("freelancer.demo2@trustedwork.tn", "Frontend", "Pro", "Free123!", Role.FREELANCER, 10000004);

            log.info("Demo users seeded successfully.");
        };
    }

    private void createUser(String email, String first, String last, String rawPass, Role role, Integer cin) {
        User u = new User();
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPass));
        u.setCin(cin);
        u.setPhone("00000000");
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setKycStatus(KycStatus.APPROVED);
        u.setEnabled(true);
        u.setAccountNonLocked(true);
        u.setTwoFactorEnabled(false);
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
    }
}
