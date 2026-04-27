package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.KycReviewRequest;
import tn.esprit.userservice.dto.KycSubmitRequest;
import tn.esprit.userservice.dto.UserDTO;
import tn.esprit.userservice.entity.AuditEventType;
import tn.esprit.userservice.entity.KycStatus;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.KycAlreadySubmittedException;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.mapper.UserMapper;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements IKycService {

    private final UserRepository userRepository;
    private final IAuditService auditService;
    private final ITrustLevelService trustLevelService;
    private final UserMapper userMapper;

    // ==================== HELPER ====================

    private User findUserByCinOrThrow(Integer cin) {
        return userRepository.findByCin(cin)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec le CIN : " + cin));
    }

    // ==================== SUBMIT KYC ====================

    @Override
    public UserDTO submitKyc(Integer cin, KycSubmitRequest request) {
        User user = findUserByCinOrThrow(cin);

        if (user.getKycStatus() == KycStatus.APPROVED) {
            throw new KycAlreadySubmittedException("KYC déjà approuvé pour cet utilisateur");
        }

        if (user.getKycStatus() == KycStatus.IN_REVIEW) {
            throw new KycAlreadySubmittedException("KYC déjà soumis et en cours de révision");
        }

        user.setKycStatus(KycStatus.IN_REVIEW);
        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(user);

        // Audit de la soumission KYC
        auditService.log(
                AuditEventType.KYC_SUBMITTED,
                user.getEmail(),
                user.getEmail(),
                "CIN soumis : " + request.getCinNumber(),
                null
        );

        log.info("KYC soumis par l'utilisateur : {}", user.getEmail());

        return userMapper.toDTO(updated);
    }

    // ==================== REVIEW KYC (ADMIN) ====================

    @Override
    public UserDTO reviewKyc(Integer cin, KycReviewRequest request, String adminEmail) {
        User user = findUserByCinOrThrow(cin);

        String decision = request.getDecision().toUpperCase();

        if ("APPROVED".equals(decision)) {
            user.setKycStatus(KycStatus.APPROVED);
        } else if ("REJECTED".equals(decision)) {
            user.setKycStatus(KycStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("La décision doit être APPROVED ou REJECTED");
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(user);

        // Recalcul du Trust Level après décision KYC
        trustLevelService.computeAndSave(updated);

        // Audit de la décision KYC
        AuditEventType eventType = "APPROVED".equals(decision)
                ? AuditEventType.KYC_APPROVED
                : AuditEventType.KYC_REJECTED;

        auditService.log(
                eventType,
                adminEmail,
                user.getEmail(),
                "Motif de rejet : " + request.getRejectReason(),
                null
        );

        log.info("KYC {} pour l'utilisateur {} par l'admin {}",
                decision, user.getEmail(), adminEmail);

        return userMapper.toDTO(updated);
    }

    // ==================== GET PENDING KYC ====================

    @Override
    public List<UserDTO> getPendingKycRequests() {
        return userRepository.findByKycStatus(KycStatus.IN_REVIEW).stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ==================== GET KYC STATUS ====================

    @Override
    public UserDTO getKycStatus(Integer cin) {
        User user = findUserByCinOrThrow(cin);
        return userMapper.toDTO(user);
    }
}