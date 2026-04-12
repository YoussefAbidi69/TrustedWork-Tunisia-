package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.dto.response.SkillGapResponse;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Algorithme de détection des gaps de compétences
 *
 * Compare les skills du freelancer avec les Top 10 skills
 * les plus présents sur la plateforme TrustedWork Tunisia
 */
@Service
@RequiredArgsConstructor
public class SkillGapServiceImpl implements ISkillGapService {

    private final SkillRepository skillRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public SkillGapResponse detectSkillGaps(Long userId) {
        Long profileId = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"))
                .getId();

        // Skills actuels du freelancer
        List<String> mySkills = skillRepository.findByProfileId(profileId)
                .stream()
                .map(Skill::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        // Top 10 skills les plus présents sur la plateforme
        List<String> topSkills = skillRepository.findTopSkillsRaw()
                .stream()
                .limit(10)
                .map(row -> ((String) row[0]).toLowerCase())
                .collect(Collectors.toList());

        // Gaps = top skills que le freelancer n'a pas
        List<String> gapSkills = topSkills.stream()
                .filter(s -> !mySkills.contains(s))
                .collect(Collectors.toList());

        return SkillGapResponse.builder()
                .mySkills(mySkills)
                .topSkills(topSkills)
                .gapSkills(gapSkills)
                .gapCount(gapSkills.size())
                .build();
    }
}