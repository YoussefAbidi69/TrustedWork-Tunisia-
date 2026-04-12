package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.dto.response.CareerPathResponse;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Algorithme de recommandation de parcours de carrière (Rule-based AI)
 *
 * Parcours prédéfinis adaptés au marché tunisien :
 * - Backend Java  : Spring Boot → Docker → Kubernetes → Microservices
 * - Frontend      : Angular → React → TypeScript → UI/UX
 * - Data          : Python → SQL → Machine Learning → Pandas
 * - DevOps        : Linux → Docker → Kubernetes → CI/CD
 * - Mobile        : Flutter → React Native → Android → iOS
 */
@Service
@RequiredArgsConstructor
public class CareerPathServiceImpl implements ICareerPathService {

    private final FreelancerProfileRepository profileRepository;
    private final SkillRepository skillRepository;

    // Définition des parcours de carrière adaptés au marché tunisien
    private static final Map<String, List<String>> CAREER_PATHS = new LinkedHashMap<>();
    private static final Map<String, String> CAREER_DESCRIPTIONS = new HashMap<>();

    static {
        CAREER_PATHS.put("Backend Java",
                Arrays.asList("Java", "Spring Boot", "Spring Security", "JPA", "MySQL", "Docker", "Kubernetes", "Microservices"));
        CAREER_PATHS.put("Frontend",
                Arrays.asList("HTML", "CSS", "JavaScript", "TypeScript", "Angular", "React", "UI/UX"));
        CAREER_PATHS.put("Data",
                Arrays.asList("Python", "SQL", "Pandas", "NumPy", "Machine Learning", "Scikit-learn", "TensorFlow"));
        CAREER_PATHS.put("DevOps",
                Arrays.asList("Linux", "Bash", "Docker", "Kubernetes", "Jenkins", "Ansible", "CI/CD", "Terraform"));
        CAREER_PATHS.put("Mobile",
                Arrays.asList("Flutter", "Dart", "React Native", "Android", "iOS", "Firebase"));

        CAREER_DESCRIPTIONS.put("Backend Java", "Développeur backend Java spécialisé en microservices et cloud");
        CAREER_DESCRIPTIONS.put("Frontend", "Développeur frontend moderne avec frameworks JavaScript");
        CAREER_DESCRIPTIONS.put("Data", "Ingénieur data et Machine Learning");
        CAREER_DESCRIPTIONS.put("DevOps", "Ingénieur DevOps et infrastructure cloud");
        CAREER_DESCRIPTIONS.put("Mobile", "Développeur mobile cross-platform");
    }

    @Override
    public CareerPathResponse recommendCareerPath(Long userId) {
        profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));

        // Récupérer les skills de l'utilisateur en minuscules pour comparaison
        List<Skill> skills = skillRepository.findByProfileId(
                profileRepository.findByUserId(userId).get().getId()
        );
        List<String> userSkills = skills.stream()
                .map(s -> s.getName().toLowerCase())
                .collect(Collectors.toList());

        // Trouver le parcours avec le plus de correspondances
        String bestPath = "Backend Java";
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : CAREER_PATHS.entrySet()) {
            long matchCount = entry.getValue().stream()
                    .filter(s -> userSkills.contains(s.toLowerCase()))
                    .count();
            if (matchCount > bestScore) {
                bestScore = (int) matchCount;
                bestPath = entry.getKey();
            }
        }

        List<String> pathSkills = CAREER_PATHS.get(bestPath);

        // Skills déjà acquis sur ce parcours
        List<String> currentSkills = pathSkills.stream()
                .filter(s -> userSkills.contains(s.toLowerCase()))
                .collect(Collectors.toList());

        // Skills manquants sur ce parcours
        List<String> missingSkills = pathSkills.stream()
                .filter(s -> !userSkills.contains(s.toLowerCase()))
                .collect(Collectors.toList());

        // Prochaines étapes = les 3 premiers skills manquants
        List<String> nextSteps = missingSkills.stream()
                .limit(3)
                .collect(Collectors.toList());

        return CareerPathResponse.builder()
                .detectedPath(bestPath)
                .description(CAREER_DESCRIPTIONS.get(bestPath))
                .nextSteps(nextSteps)
                .currentSkills(currentSkills)
                .missingSkills(missingSkills)
                .build();
    }
}