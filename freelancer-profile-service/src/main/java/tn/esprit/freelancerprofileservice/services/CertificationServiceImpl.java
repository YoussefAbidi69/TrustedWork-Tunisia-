package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.util.List;

/**
 * Implémentation du service de gestion des certifications
 */
@Service
@RequiredArgsConstructor
public class CertificationServiceImpl implements ICertificationService {

    private final CertificationRepository certificationRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public Certification addCertification(Long userId, Certification certification) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        certification.setProfile(profile);
        certification.setIsExpired(false);
        return certificationRepository.save(certification);
    }

    @Override
    public List<Certification> getMyCertifications(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        return certificationRepository.findByProfileId(profile.getId());
    }

    @Override
    public void deleteCertification(Long certId, Long userId) {
        Certification cert = certificationRepository.findById(certId)
                .orElseThrow(() -> new RuntimeException("Certification introuvable"));
        if (!cert.getProfile().getUserId().equals(userId)) {
            throw new RuntimeException("Action non autorisée");
        }
        certificationRepository.delete(cert);
    }
}