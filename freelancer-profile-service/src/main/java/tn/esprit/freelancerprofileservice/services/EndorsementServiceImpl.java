package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.Endorsement;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;

import java.util.List;

/**
 * Implémentation du service d'endorsement avec protection anti-spam
 */
@Service
@RequiredArgsConstructor
public class EndorsementServiceImpl implements IEndorsementService {

    private final EndorsementRepository endorsementRepository;
    private final SkillRepository skillRepository;

    @Override
    public Endorsement addEndorsement(Long skillId, Long endorserId, String comment) {
        if (endorsementRepository.existsByEndorserIdAndSkillId(endorserId, skillId)) {
            throw new DuplicateResourceException("Vous avez déjà validé cette compétence");
        }
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", skillId));

        if (skill.getProfile().getUserId().equals(endorserId)) {
            throw new InvalidDataException("Vous ne pouvez pas valider vos propres compétences");
        }
        Endorsement endorsement = Endorsement.builder()
                .endorserId(endorserId)
                .skill(skill)
                .comment(comment)
                .build();
        return endorsementRepository.save(endorsement);
    }

    @Override
    public List<Endorsement> getEndorsementsBySkill(Long skillId) {
        return endorsementRepository.findBySkillId(skillId);
    }

    @Override
    public long countEndorsements(Long skillId) {
        return endorsementRepository.countBySkillId(skillId);
    }
}