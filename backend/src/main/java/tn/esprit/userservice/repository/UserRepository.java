package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.id NOT IN (SELECT am.user.id FROM AgencyMember am WHERE am.agency.id = :agencyId)")
    List<User> findAvailableFreelancersForAgency(@Param("agencyId") Long agencyId, @Param("role") Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role " +
           "AND u.id NOT IN (SELECT am.user.id FROM AgencyMember am WHERE am.agency.id = :agencyId) " +
           "AND u.id NOT IN (SELECT ai.receiver.id FROM AgencyInvitation ai WHERE ai.agency.id = :agencyId AND ai.status = 'PENDING') " +
           "AND (:skill IS NULL OR LOWER(u.skills) LIKE LOWER(CONCAT('%', :skill, '%'))) " +
           "AND (:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> findAvailableFreelancersForAgencyFiltered(@Param("agencyId") Long agencyId,
                                                         @Param("role") Role role,
                                                         @Param("skill") String skill,
                                                         @Param("search") String search);
}