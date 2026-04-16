package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;

/**
 * Calcul du score d'authenticité d'une compétence.
 *
 * Score sur 100 :
 * - portfolio evidence : 40%
 * - exam score         : 35%
 * - endorsements       : 25%
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SkillAuthenticityServiceImpl implements ISkillAuthenticityService {

    private final SkillRepository skillRepository;
    private final EndorsementRepository endorsementRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    @Override
    public double calculateAuthenticityScore(Long skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", skillId));

        Long profileId = skill.getProfile().getId();

        long portfolioCount = portfolioItemRepository.countByProfileId(profileId);
        double portfolioEvidence = Math.min(portfolioCount / 10.0, 1.0);

        double rawExamScore = skill.getExamScore() != null ? skill.getExamScore() : 0.0;
        double examScoreNormalized = rawExamScore > 1 ? rawExamScore / 100.0 : rawExamScore;
        examScoreNormalized = Math.max(0.0, Math.min(examScoreNormalized, 1.0));

        long endorsementCount = endorsementRepository.countBySkillId(skillId);
        double endorsementRatio = Math.min(endorsementCount / 10.0, 1.0);

        double weightedScore = (portfolioEvidence * 0.40)
                + (examScoreNormalized * 0.35)
                + (endorsementRatio * 0.25);

        double finalScore = Math.round(weightedScore * 10000.0) / 100.0;

        skill.setAuthenticityScore(finalScore);
        skill.setEndorsementCount((int) endorsementCount);
        skillRepository.save(skill);

        return finalScore;
    }

    @Override
    public void recalculateAllScores(Long profileId) {
        List<Skill> skills = skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(profileId);
        skills.forEach(skill -> calculateAuthenticityScore(skill.getId()));
    }
}