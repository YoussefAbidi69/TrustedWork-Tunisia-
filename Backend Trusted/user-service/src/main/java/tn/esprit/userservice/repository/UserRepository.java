package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    // ================= AUTH =================
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByCin(Integer cin);

    boolean existsByCin(Integer cin);

    // ================= FILTERS =================
    List<User> findByAccountStatus(AccountStatus accountStatus);

    List<User> findByKycStatus(KycStatus kycStatus);

    List<User> findByEnabled(boolean enabled);

    // ================= SEARCH =================
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    // ================= ROLE =================
    List<User> findByRole(Role role);

    // ================= STATS =================
    long countByRole(Role role);

    long countByAccountStatus(AccountStatus status);

    long countByKycStatus(KycStatus status);

    // Comptes verrouillés — utilisé par le scheduler de déverrouillage
    List<User> findByAccountNonLockedFalse();
}