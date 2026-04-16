package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.WorkExperience;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.WorkExperienceRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkExperienceServiceImpl implements IWorkExperienceService {

    private static final String EXP_NOT_FOUND = "Expérience introuvable";

    private final WorkExperienceRepository workExperienceRepository;
    private final FreelancerProfileRepository profileRepository;
    private final ICompletenessService completenessService;

    @Override
    public WorkExperience addWorkExperience(Long userId, WorkExperience experience) {
        FreelancerProfile profile = getProfile(userId);

        validateExperience(experience, profile.getId(), null);

        experience.setProfile(profile);

        WorkExperience saved = workExperienceRepository.save(experience);
        completenessService.calculateCompleteness(userId);

        return saved;
    }

    @Override
    public WorkExperience updateWorkExperience(Long expId, Long userId, WorkExperience updates) {
        FreelancerProfile profile = getProfile(userId);

        WorkExperience existing = workExperienceRepository
                .findByIdAndProfileId(expId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException(EXP_NOT_FOUND));

        validateExperience(updates, profile.getId(), expId);

        existing.setJobTitle(updates.getJobTitle());
        existing.setCompany(updates.getCompany());
        existing.setLocation(updates.getLocation());
        existing.setDescription(updates.getDescription());
        existing.setStartDate(updates.getStartDate());
        existing.setEndDate(updates.getEndDate());
        existing.setIsCurrent(updates.getIsCurrent());

        WorkExperience saved = workExperienceRepository.save(existing);
        completenessService.calculateCompleteness(userId);

        return saved;
    }

    @Override
    public List<WorkExperience> getMyWorkExperiences(Long userId) {
        FreelancerProfile profile = getProfile(userId);
        return workExperienceRepository.findByProfileIdOrderByIsCurrentDescStartDateDesc(profile.getId());
    }

    @Override
    public WorkExperience getWorkExperienceById(Long expId, Long userId) {
        FreelancerProfile profile = getProfile(userId);

        return workExperienceRepository.findByIdAndProfileId(expId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException(EXP_NOT_FOUND));
    }

    @Override
    public void deleteWorkExperience(Long expId, Long userId) {
        FreelancerProfile profile = getProfile(userId);

        WorkExperience exp = workExperienceRepository.findByIdAndProfileId(expId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException(EXP_NOT_FOUND));

        workExperienceRepository.delete(exp);
        completenessService.calculateCompleteness(userId);
    }

    @Override
    public Long getTotalExperienceInMonths(Long userId) {
        FreelancerProfile profile = getProfile(userId);

        List<WorkExperience> experiences =
                workExperienceRepository.findByProfileIdOrderByIsCurrentDescStartDateDesc(profile.getId());

        long total = 0;

        for (WorkExperience exp : experiences) {
            LocalDate start = exp.getStartDate();
            LocalDate end = exp.getEndDate() != null ? exp.getEndDate() : LocalDate.now();

            long months = ChronoUnit.MONTHS.between(start, end);
            total += Math.max(months, 0);
        }

        return total;
    }

    private FreelancerProfile getProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil introuvable"));
    }

    private void validateExperience(WorkExperience exp, Long profileId, Long expId) {
        if (exp.getJobTitle() == null || exp.getJobTitle().isBlank()) {
            throw new InvalidDataException("Titre du poste obligatoire");
        }
        if (exp.getCompany() == null || exp.getCompany().isBlank()) {
            throw new InvalidDataException("Entreprise obligatoire");
        }
        validateDates(exp);
        validateNoDuplicate(exp, profileId, expId);
    }

    private void validateDates(WorkExperience exp) {
        if (exp.getStartDate() == null) {
            throw new InvalidDataException("Date de début obligatoire");
        }
        if (exp.getStartDate().isAfter(LocalDate.now())) {
            throw new InvalidDataException("Date de début invalide");
        }
        if (Boolean.TRUE.equals(exp.getIsCurrent())) {
            exp.setEndDate(null);
            return;
        }
        if (exp.getEndDate() == null) {
            throw new InvalidDataException("Date de fin obligatoire");
        }
        if (exp.getEndDate().isAfter(LocalDate.now())) {
            throw new InvalidDataException("Date de fin invalide");
        }
        if (exp.getStartDate().isAfter(exp.getEndDate())) {
            throw new InvalidDataException("Dates incohérentes");
        }
    }

    private void validateNoDuplicate(WorkExperience exp, Long profileId, Long expId) {
        boolean exists;
        if (expId == null) {
            exists = workExperienceRepository
                    .existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDate(
                            profileId, exp.getJobTitle(), exp.getCompany(), exp.getStartDate());
        } else {
            exists = workExperienceRepository
                    .existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDateAndIdNot(
                            profileId, exp.getJobTitle(), exp.getCompany(), exp.getStartDate(), expId);
        }
        if (exists) {
            throw new DuplicateResourceException("Expérience déjà existante");
        }
    }
}