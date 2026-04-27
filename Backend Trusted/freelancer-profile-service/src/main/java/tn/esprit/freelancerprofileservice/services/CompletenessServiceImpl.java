package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.dto.response.CompletenessResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Algorithme de calcul du score de complétude du profil
 *
 * Barème :
 * - Bio renseignée          : 15 pts
 * - Avatar uploadé          : 10 pts
 * - Skills (max 5 skills)   : 5 pts par skill → max 25 pts
 * - Portfolio (max 4 items) : 5 pts par item  → max 20 pts
 * - Certification valide    : 15 pts
 * - Expérience pro          : 15 pts
 * Total maximum             : 100 pts
 */
@Service
@RequiredArgsConstructor
public class CompletenessServiceImpl implements ICompletenessService {

    private final FreelancerProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final CertificationRepository certificationRepository;
    private final WorkExperienceRepository workExperienceRepository;

    @Override
    public CompletenessResponse calculateCompleteness(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));

        Long profileId = profile.getId();
        List<String> suggestions = new ArrayList<>();
        int total = 0;

        // 1. Bio (15 pts)
        int bioScore = 0;
        if (profile.getBio() != null && !profile.getBio().isBlank()) {
            bioScore = 15;
        } else {
            suggestions.add("Rédigez une bio pour présenter votre expertise (+15 pts)");
        }
        total += bioScore;

        // 2. Avatar (10 pts)
        int avatarScore = 0;
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) {
            avatarScore = 10;
        } else {
            suggestions.add("Ajoutez une photo de profil professionnelle (+10 pts)");
        }
        total += avatarScore;

        // 3. Skills (5 pts par skill, max 25 pts)
        long skillCount = skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(profileId).size();
        int skillsScore = (int) Math.min(skillCount * 5, 25);
        if (skillCount < 5) {
            suggestions.add("Ajoutez " + (5 - skillCount) + " skill(s) pour atteindre le maximum (+25 pts)");
        }
        total += skillsScore;

        // 4. Portfolio (5 pts par item, max 20 pts)
        long portfolioCount = portfolioItemRepository.countByProfileId(profileId);
        int portfolioScore = (int) Math.min(portfolioCount * 5, 20);
        if (portfolioCount < 4) {
            suggestions.add("Ajoutez " + (4 - portfolioCount) + " projet(s) portfolio (+20 pts)");
        }
        total += portfolioScore;

        // 5. Certification valide (15 pts)
        int certifScore = 0;
        long validCertifCount = certificationRepository.findByProfileIdAndIsExpiredFalse(profileId).size();
        if (validCertifCount > 0) {
            certifScore = 15;
        } else {
            suggestions.add("Ajoutez une certification valide (+15 pts)");
        }
        total += certifScore;

        // 6. Expérience professionnelle (15 pts)
        int workExpScore = 0;
        long workExpCount = workExperienceRepository.countByProfileId(profileId);
        if (workExpCount > 0) {
            workExpScore = 15;
        } else {
            suggestions.add("Ajoutez votre expérience professionnelle (+15 pts)");
        }
        total += workExpScore;

        // Sauvegarde du score en base
        profile.setCompletenessScore(total);
        profileRepository.save(profile);

        return CompletenessResponse.builder()
                .score(total)
                .bioScore(bioScore)
                .avatarScore(avatarScore)
                .skillsScore(skillsScore)
                .portfolioScore(portfolioScore)
                .certifScore(certifScore)
                .workExpScore(workExpScore)
                .suggestions(suggestions)
                .build();
    }
}