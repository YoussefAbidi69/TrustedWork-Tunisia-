package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.WorkExperience;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.WorkExperienceRepository;

import java.util.List;

/**
 * Implémentation du service des expériences professionnelles
 */
@Service
@RequiredArgsConstructor
public class WorkExperienceServiceImpl implements IWorkExperienceService {

    private final WorkExperienceRepository workExperienceRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public WorkExperience addWorkExperience(Long userId, WorkExperience experience) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        experience.setProfile(profile);
        return workExperienceRepository.save(experience);
    }

    @Override
    public List<WorkExperience> getMyWorkExperiences(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        return workExperienceRepository.findByProfileId(profile.getId());
    }

    @Override
    public void deleteWorkExperience(Long expId, Long userId) {
        WorkExperience exp = workExperienceRepository.findById(expId)
                .orElseThrow(() -> new RuntimeException("Expérience introuvable"));
        if (!exp.getProfile().getUserId().equals(userId)) {
            throw new RuntimeException("Action non autorisée");
        }
        workExperienceRepository.delete(exp);
    }
}