package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.dto.response.SkillGapResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillGapServiceImplTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private FreelancerProfileRepository profileRepository;

    @InjectMocks
    private SkillGapServiceImpl skillGapService;

    private FreelancerProfile profile;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Backend Developer")
                .build();
    }

    @Test
    void detectSkillGaps_shouldReturnGaps_whenMissingTopSkills() {
        Skill mySkill = Skill.builder()
                .name("Java")
                .normalizedName("java")
                .category(SkillCategory.BACKEND)
                .authenticityScore(80.0)
                .profile(profile)
                .build();

        Object[] topRow1 = new Object[]{"Docker"};
        Object[] topRow2 = new Object[]{"Java"};
        Object[] topRow3 = new Object[]{"Kubernetes"};

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of(mySkill));
        when(skillRepository.findTopSkillsRaw()).thenReturn(List.of(topRow1, topRow2, topRow3));

        SkillGapResponse response = skillGapService.detectSkillGaps(100L);

        assertThat(response).isNotNull();
        assertThat(response.getMySkills()).containsExactly("java");
        assertThat(response.getTopSkills()).containsExactly("docker", "java", "kubernetes");
        assertThat(response.getGapSkills()).containsExactly("docker", "kubernetes");
        assertThat(response.getGapCount()).isEqualTo(2);
    }

    @Test
    void detectSkillGaps_shouldReturnNoGaps_whenAllTopSkillsOwned() {
        Skill s1 = Skill.builder()
                .name("Docker")
                .normalizedName("docker")
                .profile(profile)
                .category(SkillCategory.DEVOPS)
                .build();

        Skill s2 = Skill.builder()
                .name("Java")
                .normalizedName("java")
                .profile(profile)
                .category(SkillCategory.BACKEND)
                .build();

        Object[] topRow1 = new Object[]{"Docker"};
        Object[] topRow2 = new Object[]{"Java"};

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of(s1, s2));
        when(skillRepository.findTopSkillsRaw()).thenReturn(List.of(topRow1, topRow2));

        SkillGapResponse response = skillGapService.detectSkillGaps(100L);

        assertThat(response).isNotNull();
        assertThat(response.getMySkills()).containsExactly("docker", "java");
        assertThat(response.getTopSkills()).containsExactly("docker", "java");
        assertThat(response.getGapSkills()).isEmpty();
        assertThat(response.getGapCount()).isZero();
    }

    @Test
    void detectSkillGaps_shouldReturnAllGaps_whenNoSkillsOwned() {
        Object[] topRow1 = new Object[]{"Docker"};
        Object[] topRow2 = new Object[]{"Kubernetes"};

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of());
        when(skillRepository.findTopSkillsRaw()).thenReturn(List.of(topRow1, topRow2));

        SkillGapResponse response = skillGapService.detectSkillGaps(100L);

        assertThat(response).isNotNull();
        assertThat(response.getMySkills()).isEmpty();
        assertThat(response.getTopSkills()).containsExactly("docker", "kubernetes");
        assertThat(response.getGapSkills()).containsExactly("docker", "kubernetes");
        assertThat(response.getGapCount()).isEqualTo(2);
    }

    @Test
    void detectSkillGaps_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillGapService.detectSkillGaps(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void detectSkillGaps_shouldLimitTopSkillsTo10() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of());

        List<Object[]> manyTopSkills = List.of(
                new Object[]{"Skill1"},
                new Object[]{"Skill2"},
                new Object[]{"Skill3"},
                new Object[]{"Skill4"},
                new Object[]{"Skill5"},
                new Object[]{"Skill6"},
                new Object[]{"Skill7"},
                new Object[]{"Skill8"},
                new Object[]{"Skill9"},
                new Object[]{"Skill10"},
                new Object[]{"Skill11"},
                new Object[]{"Skill12"}
        );

        when(skillRepository.findTopSkillsRaw()).thenReturn(manyTopSkills);

        SkillGapResponse response = skillGapService.detectSkillGaps(100L);

        assertThat(response).isNotNull();
        assertThat(response.getTopSkills()).hasSize(10);
        assertThat(response.getGapSkills()).hasSize(10);
        assertThat(response.getGapCount()).isEqualTo(10);
    }
}