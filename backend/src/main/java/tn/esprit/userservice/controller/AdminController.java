package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AdminCreateUserRequest;
import tn.esprit.userservice.dto.UserDTO;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.AuditLog;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.repository.AuditLogRepository;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.service.IUserService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only operations: user management, suspend, activate, audit")
// Toutes les routes /admin/** sont réservées aux ADMIN.
// La règle de classe couvre tous les endpoints ; les méthodes peuvent affiner si besoin.
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IUserService userService;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    // ==================== USER MANAGEMENT ====================

    @PostMapping("/users")
    @Operation(summary = "Admin creates a new user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(@RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(201).body(userService.createUserByAdmin(request));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/users/role/{role}")
    @Operation(summary = "List users by role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/users/status/{status}")
    @Operation(summary = "List users by account status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getUsersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(userService.getUsersByStatus(status));
    }

    @PutMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend a user account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> suspendUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setAccountNonLocked(false);
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User " + user.getEmail() + " suspended successfully"
        ));
    }

    @PutMapping("/users/{id}/activate")
    @Operation(summary = "Reactivate a suspended user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> activateUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setAccountNonLocked(true);
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User " + user.getEmail() + " activated successfully"
        ));
    }

    @DeleteMapping("/users/{cin}")
    @Operation(summary = "Soft delete a user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Integer cin) {
        userService.deleteUser(cin);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // ==================== DASHBOARD STATS ====================

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {

        Map<String, Object> stats = new HashMap<>();

        long totalFreelancers = userRepository.countByRole(Role.FREELANCER);
        long totalClients = userRepository.countByRole(Role.CLIENT);
        long totalUsers = userRepository.count();

        long activeUsers = userRepository.countByAccountStatus(AccountStatus.ACTIVE);
        long suspendedUsers = userRepository.countByAccountStatus(AccountStatus.SUSPENDED);

        long kycPending = userRepository.countByKycStatus(KycStatus.PENDING);
        long kycApproved = userRepository.countByKycStatus(KycStatus.APPROVED);
        long kycRejected = userRepository.countByKycStatus(KycStatus.REJECTED);

        stats.put("totalUsers", totalUsers);
        stats.put("totalFreelancers", totalFreelancers);
        stats.put("totalClients", totalClients);
        stats.put("activeUsers", activeUsers);
        stats.put("suspendedUsers", suspendedUsers);
        stats.put("kycPending", kycPending);
        stats.put("kycApproved", kycApproved);
        stats.put("kycRejected", kycRejected);

        return ResponseEntity.ok(stats);
    }

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit-logs")
    @Operation(summary = "Get all audit logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/audit-logs/user/{email}")
    @Operation(summary = "Get audit logs for a specific user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String email) {
        return ResponseEntity.ok(auditLogRepository.findByTargetUserOrderByCreatedAtDesc(email));
    }
}