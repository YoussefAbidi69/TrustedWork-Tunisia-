package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;
import tn.esprit.freelancerprofileservice.repositories.*;

import java.util.List;

/**
 * Implémentation du service de gestion des compétences
 */
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements ISkillService {

    private final SkillRepository skillRepository;
    private final FreelancerProfileRepository profileRepository;
    private final EndorsementRepository endorsementRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    @Override
    public Skill addSkill(Long userId, Skill skill) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        if (skillRepository.existsByProfileIdAndName(profile.getId(), skill.getName())) {
            throw new RuntimeException("Ce skill existe déjà sur votre profil");
        }
        skill.setProfile(profile);
        skill.setLevel(SkillLevel.JUNIOR);
        skill.setAuthenticityScore(0.0);
        return skillRepository.save(skill);
    }

    @Override
    public List<Skill> getMySkills(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        return skillRepository.findByProfileId(profile.getId());
    }

    @Override
    public void deleteSkill(Long skillId, Long userId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill introuvable"));
        if (!skill.getProfile().getUserId().equals(userId)) {
            throw new RuntimeException("Action non autorisée");
        }
        skillRepository.delete(skill);
    }

    @Override
    public Skill upgradeSkillLevelIfEligible(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new RuntimeException("Skill introuvable"));
        long endorsementCount = endorsementRepository.countBySkillId(skillId);
        long portfolioCount = portfolioItemRepository.countByProfileId(skill.getProfile().getId());
        if (endorsementCount >= 10) {
            skill.setLevel(SkillLevel.EXPERT);
        } else if (endorsementCount >= 5 && portfolioCount >= 3) {
            skill.setLevel(SkillLevel.CONFIRMED);
        }
        return skillRepository.save(skill);
    }
}