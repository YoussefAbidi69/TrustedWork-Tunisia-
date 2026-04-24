package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.dto.response.SkillGapResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;

/**
 * Détection des gaps de compétences.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillGapServiceImpl implements ISkillGapService {

    private final SkillRepository skillRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public SkillGapResponse detectSkillGaps(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FreelancerProfile", userId));

        List<String> mySkills = skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(profile.getId())
                .stream()
                .map(Skill::getName)
                .map(name -> name.toLowerCase().trim())
                .distinct()
                .toList();

        List<String> topSkills = skillRepository.findTopSkillsRaw()
                .stream()
                .limit(10)
                .map(row -> ((String) row[0]).toLowerCase().trim())
                .distinct()
                .toList();

        List<String> gapSkills = topSkills.stream()
                .filter(skill -> !mySkills.contains(skill))
                .toList();

        return SkillGapResponse.builder()
                .mySkills(mySkills)
                .topSkills(topSkills)
                .gapSkills(gapSkills)
                .gapCount(gapSkills.size())
                .build();
    }
}