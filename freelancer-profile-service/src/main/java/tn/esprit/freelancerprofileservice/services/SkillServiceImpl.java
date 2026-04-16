package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.dto.response.SkillResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.exceptions.UnauthorizedActionException;
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;

/**
 * Gestion des compétences du freelancer.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SkillServiceImpl implements ISkillService {

    private static final int MAX_SKILLS_PER_PROFILE = 30;
    private static final String SKILL_ENTITY = "Skill";

    private final SkillRepository skillRepository;
    private final FreelancerProfileRepository profileRepository;
    private final EndorsementRepository endorsementRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final ISkillAuthenticityService skillAuthenticityService;

    @Override
    public Skill addSkill(Long userId, Skill skill) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FreelancerProfile", userId));

        if (skillRepository.countByProfileId(profile.getId()) >= MAX_SKILLS_PER_PROFILE) {
            throw new InvalidDataException("Vous avez atteint la limite de 30 skills");
        }

        String normalizedName = normalizeSkillName(skill.getName());

        if (skillRepository.existsByProfileIdAndNormalizedName(profile.getId(), normalizedName)) {
            throw new DuplicateResourceException("Ce skill existe déjà sur votre profil");
        }

        skill.setProfile(profile);
        skill.setName(cleanDisplayName(skill.getName()));
        skill.setNormalizedName(normalizedName);
        skill.setLevel(skill.getLevel() != null ? skill.getLevel() : SkillLevel.JUNIOR);
        skill.setAuthenticityScore(0.0);
        skill.setExamScore(skill.getExamScore() != null ? skill.getExamScore() : 0.0);
        skill.setEndorsementCount(0);

        Skill savedSkill = skillRepository.save(skill);
        skillAuthenticityService.calculateAuthenticityScore(savedSkill.getId());

        return skillRepository.findById(savedSkill.getId())
                .orElseThrow(() -> new ResourceNotFoundException(SKILL_ENTITY, savedSkill.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Skill> getMySkills(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FreelancerProfile", userId));

        return skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(profile.getId());
    }

    @Override
    public void deleteSkill(Long skillId, Long userId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException(SKILL_ENTITY, skillId));

        if (!skill.getProfile().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("Action non autorisée");
        }

        Long profileId = skill.getProfile().getId();
        skillRepository.delete(skill);

        skillAuthenticityService.recalculateAllScores(profileId);
    }

    @Override
    public Skill upgradeSkillLevelIfEligible(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException(SKILL_ENTITY, skillId));

        long endorsementCount = endorsementRepository.countBySkillId(skillId);
        long portfolioCount = portfolioItemRepository.countByProfileId(skill.getProfile().getId());

        skill.setEndorsementCount((int) endorsementCount);

        if (endorsementCount >= 10) {
            skill.setLevel(SkillLevel.EXPERT);
        } else if (endorsementCount >= 5 && portfolioCount >= 3) {
            skill.setLevel(SkillLevel.CONFIRMED);
        } else if (endorsementCount >= 2 || skill.getAuthenticityScore() >= 50) {
            skill.setLevel(SkillLevel.INTERMEDIATE);
        } else {
            skill.setLevel(SkillLevel.JUNIOR);
        }

        Skill updatedSkill = skillRepository.save(skill);
        skillAuthenticityService.calculateAuthenticityScore(updatedSkill.getId());

        return updatedSkill;
    }

    private String normalizeSkillName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidDataException("Le nom du skill est obligatoire");
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String cleanDisplayName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }


    @Override
    public SkillResponse updateExamScore(Long skillId, Long userId, Double examScore) {
        // Vérifier appartenance au profil
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException(SKILL_ENTITY, skillId));

        if (!skill.getProfile().getUserId().equals(userId)) {
            throw new UnauthorizedActionException("Cette competence ne vous appartient pas");
        }

        // Mettre à jour le score d'examen
        skill.setExamScore(examScore);
        skillRepository.save(skill);

        // Recalcul automatique de l'authenticité
        skillAuthenticityService.calculateAuthenticityScore(skillId);

        // Retourner le skill mis à jour via le controller (mapping dans SkillController)
        Skill updated = skillRepository.findById(skillId).orElse(skill);

        return SkillResponse.builder()
                .id(updated.getId())
                .name(updated.getName())
                .category(updated.getCategory())
                .level(updated.getLevel())
                .authenticityScore(updated.getAuthenticityScore())
                .examScore(updated.getExamScore())
                .endorsementCount(updated.getEndorsementCount())
                .build();
    }
}