package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.SuspensionRecordDTO;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.repository.SuspensionRecordRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuspensionServiceImpl implements ISuspensionService {

    private final SuspensionRecordRepository suspensionRecordRepository;
    private final UserRepository userRepository;
    private final IAuditService auditService;

    // ==================== SUSPEND ====================

    @Override
    public SuspensionRecordDTO suspendUser(Long userId, String reason, String adminEmail) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("L'utilisateur est déjà suspendu");
        }

        // Création du SuspensionRecord
        SuspensionRecord record = new SuspensionRecord();
        record.setUser(user);
        record.setReason(reason);
        record.setSuspendedBy(adminEmail);
        record.setActive(true);

        SuspensionRecord saved = suspensionRecordRepository.save(record);

        // Mise à jour du statut utilisateur
        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setAccountNonLocked(false);
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Audit
        auditService.log(
                AuditEventType.USER_SUSPENDED,
                adminEmail,
                user.getEmail(),
                "Motif : " + reason,
                null
        );

        log.info("Utilisateur {} suspendu par {} — motif : {}",
                user.getEmail(), adminEmail, reason);

        return toDTO(saved);
    }

    // ==================== LIFT ====================

    @Override
    public SuspensionRecordDTO liftSuspension(Long userId, String adminEmail) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));

        if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
            throw new IllegalStateException("L'utilisateur n'est pas suspendu");
        }

        // Fermeture du SuspensionRecord actif
        SuspensionRecord record = suspensionRecordRepository
                .findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun enregistrement de suspension actif trouvé"));

        record.setActive(false);
        record.setLiftedAt(LocalDateTime.now());
        record.setLiftedBy(adminEmail);

        SuspensionRecord updated = suspensionRecordRepository.save(record);

        // Réactivation de l'utilisateur
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setAccountNonLocked(true);
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Audit
        auditService.log(
                AuditEventType.USER_ACTIVATED,
                adminEmail,
                user.getEmail(),
                "Suspension levée par " + adminEmail,
                null
        );

        log.info("Suspension levée pour {} par {}", user.getEmail(), adminEmail);

        return toDTO(updated);
    }

    // ==================== GET ====================

    @Override
    public List<SuspensionRecordDTO> getHistory(Long userId) {
        return suspensionRecordRepository
                .findByUserIdOrderBySuspendedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SuspensionRecordDTO> getAllActive() {
        return suspensionRecordRepository
                .findByActiveTrueOrderBySuspendedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ==================== MAPPER ====================

    private SuspensionRecordDTO toDTO(SuspensionRecord r) {
        return SuspensionRecordDTO.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userEmail(r.getUser().getEmail())
                .reason(r.getReason())
                .suspendedBy(r.getSuspendedBy())
                .suspendedAt(r.getSuspendedAt())
                .liftedAt(r.getLiftedAt())
                .liftedBy(r.getLiftedBy())
                .active(r.isActive())
                .build();
    }
}