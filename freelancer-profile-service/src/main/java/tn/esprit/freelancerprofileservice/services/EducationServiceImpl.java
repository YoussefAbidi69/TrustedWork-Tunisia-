package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.Education;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.EducationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.util.List;

/**
 * Implémentation du service du parcours académique
 */
@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements IEducationService {

    private final EducationRepository educationRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public Education addEducation(Long userId, Education education) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        education.setProfile(profile);
        return educationRepository.save(education);
    }

    @Override
    public List<Education> getMyEducations(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        return educationRepository.findByProfileId(profile.getId());
    }

    @Override
    public void deleteEducation(Long eduId, Long userId) {
        Education edu = educationRepository.findById(eduId)
                .orElseThrow(() -> new RuntimeException("Éducation introuvable"));
        if (!edu.getProfile().getUserId().equals(userId)) {
            throw new RuntimeException("Action non autorisée");
        }
        educationRepository.delete(edu);
    }
}