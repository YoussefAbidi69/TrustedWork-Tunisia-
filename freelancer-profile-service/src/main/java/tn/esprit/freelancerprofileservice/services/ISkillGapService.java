package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.response.SkillGapResponse;

public interface ISkillGapService {

    // Détecter les skills manquants par rapport aux top freelancers
    SkillGapResponse detectSkillGaps(Long userId);
}