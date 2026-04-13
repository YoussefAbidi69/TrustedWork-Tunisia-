package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.KycRequestDTO;
import tn.esprit.userservice.dto.KycReviewRequest;
import tn.esprit.userservice.dto.KycSubmitRequest;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.repository.KycRequestRepository;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycRequestServiceImpl implements IKycRequestService {

    private final KycRequestRepository kycRequestRepository;
    private final UserRepository userRepository;
    private final IAuditService auditService;
    private final ITrustLevelService trustLevelService;

    private static final double LIVENESS_THRESHOLD = 0.75;

    @Override
    public KycRequestDTO submitKycRequest(Long userId, KycSubmitRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));

        if (request.getCinNumber() == null || request.getCinNumber().isBlank()) {
            throw new IllegalArgumentException("Le numéro CIN est obligatoire");
        }

        boolean hasCin     = request.getCinDocumentPath()    != null && !request.getCinDocumentPath().isBlank();
        boolean hasSelfie  = request.getSelfiePath()          != null && !request.getSelfiePath().isBlank();
        boolean hasDiploma = request.getDiplomaDocumentPath() != null && !request.getDiplomaDocumentPath().isBlank();

        // Cas diplôme seul : compte APPROVED qui ajoute uniquement un diplôme
        boolean isDiplomaOnly = !hasCin && !hasSelfie && hasDiploma;

        if (!isDiplomaOnly) {
            // Soumission complète — CIN + selfie obligatoires
            if (!hasCin)    throw new IllegalArgumentException("Le document CIN est obligatoire");
            if (!hasSelfie) throw new IllegalArgumentException("Le selfie est obligatoire");
        }

        if (!hasCin && !hasSelfie && !hasDiploma) {
            throw new IllegalArgumentException("Au moins un document est requis");
        }

        // Bloquer uniquement si IN_REVIEW (pas APPROVED — on permet l'ajout diplôme)
        if (!isDiplomaOnly && kycRequestRepository.existsByUserIdAndStatus(userId, KycStatus.IN_REVIEW)) {
            throw new IllegalStateException("Une demande KYC est déjà en cours de traitement");
        }

        double livenessScore = isDiplomaOnly ? 0.0
                : computeLivenessScore(request.getCinDocumentPath(), request.getSelfiePath());

        boolean livenessPassed = isDiplomaOnly
                ? user.isLivenessPassed()
                : livenessScore >= LIVENESS_THRESHOLD;

        KycRequest kycRequest = new KycRequest();
        kycRequest.setUser(user);
        kycRequest.setCinDocumentPath(request.getCinDocumentPath());
        kycRequest.setSelfiePath(request.getSelfiePath());
        kycRequest.setDiplomaDocumentPath(request.getDiplomaDocumentPath());
        kycRequest.setLivenessScore(livenessScore);
        kycRequest.setLivenessPassed(livenessPassed);

        // Diplôme seul → statut reste APPROVED, sinon IN_REVIEW
        KycStatus newStatus = isDiplomaOnly ? KycStatus.APPROVED : KycStatus.IN_REVIEW;
        kycRequest.setStatus(newStatus);

        KycRequest saved = kycRequestRepository.save(kycRequest);

        user.setKycStatus(newStatus);
        if (!isDiplomaOnly) {
            user.setLivenessPassed(livenessPassed);
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        if (!isDiplomaOnly && livenessPassed) {
            trustLevelService.computeAndSave(user);
        }

        auditService.log(
                AuditEventType.KYC_SUBMITTED,
                user.getEmail(),
                user.getEmail(),
                isDiplomaOnly
                        ? "Diplôme ajouté : " + request.getDiplomaDocumentPath()
                        : "CIN soumis : " + request.getCinNumber()
                        + " | Liveness score : " + livenessScore
                        + " | Passé : " + livenessPassed,
                null
        );

        log.info("KYC soumis pour {} — diplomaOnly={} — score={} — passé={}",
                user.getEmail(), isDiplomaOnly, livenessScore, livenessPassed);

        return toDTO(saved);
    }

    @Override
    public KycRequestDTO reviewKycRequest(Long kycRequestId,
                                          KycReviewRequest request,
                                          String adminEmail) {

        KycRequest kycRequest = kycRequestRepository.findById(kycRequestId)
                .orElseThrow(() -> new UserNotFoundException("Demande KYC introuvable : " + kycRequestId));

        if (request.getDecision() == null || request.getDecision().isBlank()) {
            throw new IllegalArgumentException("La décision est obligatoire");
        }

        String decision = request.getDecision().trim().toUpperCase();

        if ("APPROVED".equals(decision)) {
            kycRequest.setStatus(KycStatus.APPROVED);
            kycRequest.setRejectReason(null);
        } else if ("REJECTED".equals(decision)) {
            kycRequest.setStatus(KycStatus.REJECTED);
            kycRequest.setRejectReason(request.getRejectReason());
        } else {
            throw new IllegalArgumentException("Décision invalide : doit être APPROVED ou REJECTED");
        }

        kycRequest.setReviewedBy(adminEmail);
        kycRequest.setReviewedAt(LocalDateTime.now());

        KycRequest updated = kycRequestRepository.save(kycRequest);

        User user = kycRequest.getUser();
        user.setKycStatus(kycRequest.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        trustLevelService.computeAndSave(user);

        AuditEventType eventType = "APPROVED".equals(decision)
                ? AuditEventType.KYC_APPROVED
                : AuditEventType.KYC_REJECTED;

        auditService.log(
                eventType,
                adminEmail,
                user.getEmail(),
                "Décision : " + decision + " | Motif : " + request.getRejectReason(),
                null
        );

        log.info("KYC {} pour {} par admin {}", decision, user.getEmail(), adminEmail);

        return toDTO(updated);
    }

    @Override
    public List<KycRequestDTO> getPendingRequests() {
        return kycRequestRepository
                .findByStatusOrderByCreatedAtAsc(KycStatus.IN_REVIEW)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KycRequestDTO> getHistoryByUser(Long userId) {
        return kycRequestRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public double computeLivenessScore(String cinDocumentPath, String selfiePath) {
        if (cinDocumentPath == null || cinDocumentPath.isBlank() ||
                selfiePath == null || selfiePath.isBlank()) {
            return 0.0;
        }
        double simulatedScore = 0.60 + (Math.random() * 0.39);
        return Math.round(simulatedScore * 100.0) / 100.0;
    }

    private KycRequestDTO toDTO(KycRequest k) {
        return KycRequestDTO.builder()
                .id(k.getId())
                .userId(k.getUser().getId())
                .userEmail(k.getUser().getEmail())
                .cinDocumentPath(k.getCinDocumentPath())
                .diplomaDocumentPath(k.getDiplomaDocumentPath())
                .selfiePath(k.getSelfiePath())
                .livenessScore(k.getLivenessScore())
                .livenessPassed(k.isLivenessPassed())
                .status(k.getStatus().name())
                .reviewedBy(k.getReviewedBy())
                .reviewedAt(k.getReviewedAt())
                .rejectReason(k.getRejectReason())
                .createdAt(k.getCreatedAt())
                .build();
    }
}