package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.Skill;

import java.util.List;

public interface ISkillService {
    Skill addSkill(Long userId, Skill skill);
    List<Skill> getMySkills(Long userId);
    void deleteSkill(Long skillId, Long userId);
    Skill upgradeSkillLevelIfEligible(Long skillId);
}