package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;

/**
 * Algorithme de calcul du score d'authenticité d'une compétence
 *
 * Formule :
 * score = (portfolioEvidence * 0.40) + (examScore * 0.35) + (endorsements * 0.25)
 *
 * - portfolioEvidence : ratio projets portfolio contenant ce skill (0.0 → 1.0)
 * - examScore         : score examen interne Module 04 (0.0 → 1.0)
 * - endorsements      : ratio endorsements normalisé sur 10 (0.0 → 1.0)
 */
@Service
@RequiredArgsConstructor
public class SkillAuthenticityServiceImpl implements ISkillAuthenticityService {

    private final SkillRepository skillRepository;
    private final EndorsementRepository endorsementRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    @Override
    public double calculateAuthenticityScore(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", skillId));

        Long profileId = skill.getProfile().getId();

        // 1. Portfolio evidence : nb projets / 10 (plafonné à 1.0)
        long portfolioCount = portfolioItemRepository.countByProfileId(profileId);
        double portfolioEvidence = Math.min(portfolioCount / 10.0, 1.0);

        // 2. Exam score : déjà normalisé entre 0.0 et 1.0
        double examScore = skill.getExamScore() != null ? skill.getExamScore() : 0.0;

        // 3. Endorsements : nb endorsements / 10 (plafonné à 1.0)
        long endorsementCount = endorsementRepository.countBySkillId(skillId);
        double endorsementRatio = Math.min(endorsementCount / 10.0, 1.0);

        // Calcul du score final pondéré
        double score = (portfolioEvidence * 0.40)
                + (examScore * 0.35)
                + (endorsementRatio * 0.25);

        // Arrondi à 2 décimales
        double finalScore = Math.round(score * 100.0) / 100.0;

        // Sauvegarde du score calculé
        skill.setAuthenticityScore(finalScore);
        skillRepository.save(skill);

        return finalScore;
    }

    @Override
    public void recalculateAllScores(Long profileId) {
        List<Skill> skills = skillRepository.findByProfileId(profileId);
        skills.forEach(skill -> calculateAuthenticityScore(skill.getId()));
    }
}