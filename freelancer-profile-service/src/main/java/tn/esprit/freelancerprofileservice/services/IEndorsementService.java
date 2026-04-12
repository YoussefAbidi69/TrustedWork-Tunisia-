package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.Endorsement;

import java.util.List;

public interface IEndorsementService {
    Endorsement addEndorsement(Long skillId, Long endorserId, String comment);
    List<Endorsement> getEndorsementsBySkill(Long skillId);
    long countEndorsements(Long skillId);
}