package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.freelancerprofileservice.dto.response.CareerPathResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.*;

/**
 * Recommandation simple de parcours de carrière.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerPathServiceImpl implements ICareerPathService {

    private final FreelancerProfileRepository profileRepository;
    private final SkillRepository skillRepository;

    private static final String BACKEND_JAVA = "Backend Java";

    private static final Map<String, List<String>> CAREER_PATHS = new LinkedHashMap<>();
    private static final Map<String, String> CAREER_DESCRIPTIONS = new HashMap<>();

    static {
        CAREER_PATHS.put(BACKEND_JAVA,
                Arrays.asList("Java", "Spring Boot", "Spring Security", "JPA", "MySQL", "Docker", "Kubernetes", "Microservices"));

        CAREER_PATHS.put("Frontend",
                Arrays.asList("HTML", "CSS", "JavaScript", "TypeScript", "Angular", "React", "UI/UX"));

        CAREER_PATHS.put("Data",
                Arrays.asList("Python", "SQL", "Pandas", "NumPy", "Machine Learning", "Scikit-learn", "TensorFlow"));

        CAREER_PATHS.put("DevOps",
                Arrays.asList("Linux", "Bash", "Docker", "Kubernetes", "Jenkins", "Ansible", "CI/CD", "Terraform"));

        CAREER_PATHS.put("Mobile",
                Arrays.asList("Flutter", "Dart", "React Native", "Android", "iOS", "Firebase"));

        CAREER_DESCRIPTIONS.put(BACKEND_JAVA, "Développeur backend Java spécialisé en microservices et cloud");
        CAREER_DESCRIPTIONS.put("Frontend", "Développeur frontend moderne avec frameworks JavaScript");
        CAREER_DESCRIPTIONS.put("Data", "Ingénieur data et Machine Learning");
        CAREER_DESCRIPTIONS.put("DevOps", "Ingénieur DevOps et infrastructure cloud");
        CAREER_DESCRIPTIONS.put("Mobile", "Développeur mobile cross-platform");
    }

    @Override
    public CareerPathResponse recommendCareerPath(Long userId) {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("FreelancerProfile", userId));

        List<Skill> skills = skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(profile.getId());

        List<String> userSkills = skills.stream()
                .map(Skill::getName)
                .map(name -> name.toLowerCase().trim())
                .distinct()
                .toList();

        String bestPath = BACKEND_JAVA;
        int bestScore = -1;

        for (Map.Entry<String, List<String>> entry : CAREER_PATHS.entrySet()) {
            int matchCount = (int) entry.getValue().stream()
                    .map(s -> s.toLowerCase().trim())
                    .filter(userSkills::contains)
                    .count();

            if (matchCount > bestScore) {
                bestScore = matchCount;
                bestPath = entry.getKey();
            }
        }

        List<String> pathSkills = CAREER_PATHS.get(bestPath);

        List<String> currentSkills = pathSkills.stream()
                .filter(skill -> userSkills.contains(skill.toLowerCase().trim()))
                .toList();

        List<String> missingSkills = pathSkills.stream()
                .filter(skill -> !userSkills.contains(skill.toLowerCase().trim()))
                .toList();

        List<String> nextSteps = missingSkills.stream()
                .limit(3)
                .toList();

        return CareerPathResponse.builder()
                .detectedPath(bestPath)
                .description(CAREER_DESCRIPTIONS.get(bestPath))
                .nextSteps(nextSteps)
                .currentSkills(currentSkills)
                .missingSkills(missingSkills)
                .build();
    }
}