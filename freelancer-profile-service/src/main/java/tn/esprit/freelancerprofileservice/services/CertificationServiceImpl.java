package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.dto.request.UpdateCertificationRequest;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.exceptions.UnauthorizedActionException;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Implémentation du service de gestion des certifications
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CertificationServiceImpl implements ICertificationService {

    private static final int MAX_CERTIFICATIONS_PER_PROFILE = 20;

    private final CertificationRepository certificationRepository;
    private final FreelancerProfileRepository profileRepository;
    private final ICompletenessService completenessService;

    @Override
    public Certification addCertification(Long userId, Certification certification) {
        FreelancerProfile profile = getProfileByUserId(userId);

        validateCertificationLimit(profile.getId());

        String normalizedTitle = normalize(certification.getTitle());
        String normalizedIssuer = normalize(certification.getIssuer());
        String normalizedCertificateUrl = normalize(certification.getCertificateUrl());

        if (certificationRepository.existsByProfileIdAndTitleIgnoreCaseAndIssuerIgnoreCase(
                profile.getId(),
                normalizedTitle,
                normalizedIssuer
        )) {
            throw new DuplicateResourceException("Une certification avec le même titre et le même émetteur existe déjà");
        }

        if (certification.getIssueDate() != null
                && certification.getExpiryDate() != null
                && certification.getIssueDate().isAfter(certification.getExpiryDate())) {
            throw new InvalidDataException("La date d'émission doit être antérieure ou égale à la date d'expiration");
        }

        certification.setTitle(normalizedTitle);
        certification.setIssuer(normalizedIssuer);
        certification.setCertificateUrl(normalizedCertificateUrl);
        certification.setProfile(profile);
        certification.setIsExpired(isExpired(certification.getExpiryDate()));

        Certification saved = certificationRepository.save(certification);

        recalculateCompletenessScore(profile.getUserId());

        return saved;
    }

    @Override
    public Certification updateCertification(Long certId, Long userId, UpdateCertificationRequest request) {
        Certification existing = certificationRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification introuvable"));

        if (!existing.getProfile().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("Action non autorisée");
        }

        String finalTitle = request.getTitle() != null ? normalize(request.getTitle()) : existing.getTitle();
        String finalIssuer = request.getIssuer() != null ? normalize(request.getIssuer()) : existing.getIssuer();
        String finalCertificateUrl = request.getCertificateUrl() != null
                ? normalize(request.getCertificateUrl())
                : existing.getCertificateUrl();

        LocalDate finalIssueDate = request.getIssueDate() != null
                ? request.getIssueDate()
                : existing.getIssueDate();

        LocalDate finalExpiryDate = request.getExpiryDate() != null
                ? request.getExpiryDate()
                : existing.getExpiryDate();

        if (finalIssueDate != null && finalExpiryDate != null && finalIssueDate.isAfter(finalExpiryDate)) {
            throw new InvalidDataException("La date d'émission doit être antérieure ou égale à la date d'expiration");
        }

        if (certificationRepository.existsByProfileIdAndTitleIgnoreCaseAndIssuerIgnoreCaseAndIdNot(
                existing.getProfile().getId(),
                finalTitle,
                finalIssuer,
                existing.getId()
        )) {
            throw new DuplicateResourceException("Une autre certification avec le même titre et le même émetteur existe déjà");
        }

        existing.setTitle(finalTitle);
        existing.setIssuer(finalIssuer);
        existing.setCertificateUrl(finalCertificateUrl);
        existing.setIssueDate(finalIssueDate);
        existing.setExpiryDate(finalExpiryDate);
        existing.setIsExpired(isExpired(finalExpiryDate));

        if (request.getType() != null) {
            existing.setType(request.getType());
        }

        Certification updated = certificationRepository.save(existing);

        recalculateCompletenessScore(existing.getProfile().getUserId());

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certification> getMyCertifications(Long userId) {
        FreelancerProfile profile = getProfileByUserId(userId);
        return certificationRepository.findByProfileIdOrderByIssueDateDesc(profile.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certification> getCertificationsByProfileId(Long profileId) {
        return certificationRepository.findByProfileIdOrderByIssueDateDesc(profileId);
    }

    @Override
    public void deleteCertification(Long certId, Long userId) {
        Certification cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification introuvable"));

        if (!cert.getProfile().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("Action non autorisée");
        }

        Long ownerUserId = cert.getProfile().getUserId();

        certificationRepository.delete(cert);

        recalculateCompletenessScore(ownerUserId);
    }

    private FreelancerProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable"));
    }

    private void validateCertificationLimit(Long profileId) {
        long count = certificationRepository.countByProfileId(profileId);
        if (count >= MAX_CERTIFICATIONS_PER_PROFILE) {
            throw new InvalidDataException("Limite atteinte : un profil ne peut pas avoir plus de 20 certifications");
        }
    }

    private boolean isExpired(LocalDate expiryDate) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void recalculateCompletenessScore(Long userId) {
        completenessService.calculateCompleteness(userId);
    }
}