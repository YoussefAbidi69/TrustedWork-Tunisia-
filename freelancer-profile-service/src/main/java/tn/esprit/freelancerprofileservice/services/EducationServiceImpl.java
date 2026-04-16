package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.dto.request.UpdateEducationRequest;
import tn.esprit.freelancerprofileservice.entities.Education;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.exceptions.UnauthorizedActionException;
import tn.esprit.freelancerprofileservice.repositories.EducationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.util.List;

/**
 * Implémentation du service — parcours académique
 *
 * Règles métier :
 * 1. Pas de doublon : même diplôme + même institution pour le même profil
 * 2. graduationYear validé côté DTO (@Min/@Max)
 * 3. Tri par graduationYear DESC automatique via repository
 * 4. completenessScore recalculé après chaque add/update/delete
 */
@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements IEducationService {

    private final EducationRepository educationRepository;
    private final FreelancerProfileRepository profileRepository;
    private final ICompletenessService completenessService;

    @Override
    public Education addEducation(Long userId, Education education) {
        FreelancerProfile profile = getProfile(userId);

        // Vérification doublon : même diplôme + même institution
        boolean exists = educationRepository
                .existsByDegreeIgnoreCaseAndInstitutionIgnoreCaseAndProfileId(
                        education.getDegree(),
                        education.getInstitution(),
                        profile.getId());

        if (exists) {
            throw new DuplicateResourceException(
                    "Vous avez déjà déclaré ce diplôme dans cet établissement.");
        }

        education.setProfile(profile);
        Education saved = educationRepository.save(education);

        // Recalcul du score de complétude après ajout
        completenessService.calculateCompleteness(userId);

        return saved;
    }

    @Override
    public Education updateEducation(Long eduId, Long userId, UpdateEducationRequest request) {
        Education edu = educationRepository.findById(eduId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

        // Vérifier que la formation appartient bien à cet utilisateur
        if (!edu.getProfile().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("Action non autorisée");
        }

        // Mettre à jour uniquement les champs fournis
        if (request.getDegree() != null && !request.getDegree().isBlank()) {
            edu.setDegree(request.getDegree().trim());
        }
        if (request.getInstitution() != null && !request.getInstitution().isBlank()) {
            edu.setInstitution(request.getInstitution().trim());
        }
        if (request.getFieldOfStudy() != null) {
            edu.setFieldOfStudy(request.getFieldOfStudy().trim());
        }
        if (request.getGraduationYear() != null) {
            edu.setGraduationYear(request.getGraduationYear());
        }

        Education updated = educationRepository.save(edu);

        // Recalcul du score de complétude après modification
        completenessService.calculateCompleteness(userId);

        return updated;
    }

    @Override
    public List<Education> getMyEducations(Long userId) {
        FreelancerProfile profile = getProfile(userId);
        // Retourne les formations triées par année décroissante (plus récente en premier)
        return educationRepository.findByProfileIdOrderByGraduationYearDesc(profile.getId());
    }

    @Override
    public void deleteEducation(Long eduId, Long userId) {
        Education edu = educationRepository.findById(eduId)
                .orElseThrow(() -> new ResourceNotFoundException("Formation introuvable"));

        if (!edu.getProfile().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("Action non autorisée");
        }

        educationRepository.delete(edu);

        // Recalcul du score de complétude après suppression
        completenessService.calculateCompleteness(userId);
    }

    // ─── Méthode utilitaire privée ───────────────────────────────────────────
    private FreelancerProfile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable"));
    }
}