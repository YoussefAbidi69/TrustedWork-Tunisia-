package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.dto.response.CareerPathResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.enums.SkillCategory;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerPathServiceImplTest {

    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private SkillRepository skillRepository;

    @InjectMocks
    private CareerPathServiceImpl careerPathService;

    private FreelancerProfile profile;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Java Developer")
                .build();
    }

    @Test
    void recommendCareerPath_shouldReturnBackendJava_whenJavaSkills() {
        List<Skill> skills = List.of(
                buildSkill("Java"),
                buildSkill("Spring Boot"),
                buildSkill("JPA"),
                buildSkill("Docker")
        );

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(skills);

        CareerPathResponse response = careerPathService.recommendCareerPath(100L);

        assertThat(response).isNotNull();
        assertThat(response.getDetectedPath()).isEqualTo("Backend Java");
        assertThat(response.getDescription()).contains("backend");
        assertThat(response.getCurrentSkills()).isNotEmpty();
        assertThat(response.getMissingSkills()).isNotEmpty();
    }

    @Test
    void recommendCareerPath_shouldReturnData_whenDataSkills() {
        List<Skill> skills = List.of(
                buildSkill("Python"),
                buildSkill("SQL"),
                buildSkill("Pandas"),
                buildSkill("NumPy"),
                buildSkill("Machine Learning")
        );

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(skills);

        CareerPathResponse response = careerPathService.recommendCareerPath(100L);

        assertThat(response.getDetectedPath()).isEqualTo("Data");
    }

    @Test
    void recommendCareerPath_shouldReturnDevOps_whenDevOpsSkills() {
        List<Skill> skills = List.of(
                buildSkill("Linux"),
                buildSkill("Docker"),
                buildSkill("Kubernetes"),
                buildSkill("Jenkins"),
                buildSkill("Ansible"),
                buildSkill("Terraform")
        );

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(skills);

        CareerPathResponse response = careerPathService.recommendCareerPath(100L);

        assertThat(response.getDetectedPath()).isEqualTo("DevOps");
    }

    @Test
    void recommendCareerPath_shouldLimitNextStepsToThree() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of());

        CareerPathResponse response = careerPathService.recommendCareerPath(100L);

        assertThat(response.getNextSteps()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void recommendCareerPath_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> careerPathService.recommendCareerPath(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recommendCareerPath_shouldHandleEmptySkills() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of());

        CareerPathResponse response = careerPathService.recommendCareerPath(100L);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentSkills()).isEmpty();
        assertThat(response.getDetectedPath()).isNotBlank();
    }

    private Skill buildSkill(String name) {
        return Skill.builder()
                .name(name)
                .normalizedName(name.toLowerCase())
                .category(SkillCategory.BACKEND)
                .authenticityScore(50.0)
                .profile(profile)
                .build();
    }
}
