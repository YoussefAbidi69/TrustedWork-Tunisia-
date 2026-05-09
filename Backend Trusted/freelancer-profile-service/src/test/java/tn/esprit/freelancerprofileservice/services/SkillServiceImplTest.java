package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.dto.response.SkillResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.enums.SkillCategory;
import tn.esprit.freelancerprofileservice.enums.SkillLevel;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.exceptions.UnauthorizedActionException;
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock private SkillRepository skillRepository;
    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private EndorsementRepository endorsementRepository;
    @Mock private PortfolioItemRepository portfolioItemRepository;
    @Mock private ISkillAuthenticityService skillAuthenticityService;

    @InjectMocks
    private SkillServiceImpl skillService;

    private FreelancerProfile profile;
    private Skill skill;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Backend Developer")
                .build();

        skill = Skill.builder()
                .id(10L)
                .name("Spring Boot")
                .normalizedName("spring boot")
                .category(SkillCategory.BACKEND)
                .level(SkillLevel.CONFIRMED)
                .authenticityScore(75.0)
                .examScore(80.0)
                .endorsementCount(5)
                .profile(profile)
                .build();
    }

    @Test
    void addSkill_shouldSaveAndReturn() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.countByProfileId(1L)).thenReturn(0L);
        when(skillRepository.existsByProfileIdAndNormalizedName(1L, "spring boot")).thenReturn(false);
        when(skillRepository.save(any())).thenReturn(skill);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));

        Skill result = skillService.addSkill(100L, skill);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Spring Boot");
        verify(skillAuthenticityService).calculateAuthenticityScore(10L);
    }

    @Test
    void addSkill_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.addSkill(99L, skill))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addSkill_shouldThrow_whenLimitReached() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.countByProfileId(1L)).thenReturn(30L);

        assertThatThrownBy(() -> skillService.addSkill(100L, skill))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("limite de 30");
    }

    @Test
    void addSkill_shouldThrow_whenDuplicate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.countByProfileId(1L)).thenReturn(5L);
        when(skillRepository.existsByProfileIdAndNormalizedName(1L, "spring boot")).thenReturn(true);

        assertThatThrownBy(() -> skillService.addSkill(100L, skill))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void addSkill_shouldThrow_whenNameIsBlank() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.countByProfileId(1L)).thenReturn(0L);

        Skill blankNameSkill = Skill.builder()
                .name("   ")
                .category(SkillCategory.BACKEND)
                .build();

        assertThatThrownBy(() -> skillService.addSkill(100L, blankNameSkill))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("obligatoire");
    }

    @Test
    void addSkill_shouldSetDefaultLevel_whenLevelIsNull() {
        Skill noLevelSkill = Skill.builder()
                .id(11L)
                .name("Docker")
                .normalizedName("docker")
                .category(SkillCategory.DEVOPS)
                .level(null)
                .profile(profile)
                .build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.countByProfileId(1L)).thenReturn(0L);
        when(skillRepository.existsByProfileIdAndNormalizedName(1L, "docker")).thenReturn(false);
        when(skillRepository.save(any())).thenAnswer(invocation -> {
            Skill s = invocation.getArgument(0);
            s.setId(11L);
            return s;
        });
        when(skillRepository.findById(11L)).thenReturn(Optional.of(noLevelSkill));

        Skill result = skillService.addSkill(100L, noLevelSkill);

        assertThat(result).isNotNull();
        verify(skillRepository).save(any());
    }

    @Test
    void getMySkills_shouldReturnList() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(1L)).thenReturn(List.of(skill));

        List<Skill> result = skillService.getMySkills(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Spring Boot");
    }

    @Test
    void deleteSkill_shouldDeleteSuccessfully() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));

        skillService.deleteSkill(10L, 100L);

        verify(skillRepository).delete(skill);
        verify(skillAuthenticityService).recalculateAllScores(1L);
    }

    @Test
    void deleteSkill_shouldThrow_whenNotFound() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.deleteSkill(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSkill_shouldThrow_whenUnauthorized() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));

        assertThatThrownBy(() -> skillService.deleteSkill(10L, 999L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("Action non autorisée");
    }

    @Test
    void upgradeSkillLevel_shouldSetExpert_whenEndorsementsAtLeast10() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(endorsementRepository.countBySkillId(10L)).thenReturn(10L);
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(5L);
        when(skillRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Skill result = skillService.upgradeSkillLevelIfEligible(10L);

        assertThat(result.getLevel()).isEqualTo(SkillLevel.EXPERT);
    }

    @Test
    void upgradeSkillLevel_shouldSetConfirmed_when5EndorsementsAnd3Portfolio() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(endorsementRepository.countBySkillId(10L)).thenReturn(5L);
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(3L);
        when(skillRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Skill result = skillService.upgradeSkillLevelIfEligible(10L);

        assertThat(result.getLevel()).isEqualTo(SkillLevel.CONFIRMED);
    }

    @Test
    void upgradeSkillLevel_shouldSetIntermediate_when2Endorsements() {
        skill.setAuthenticityScore(10.0);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(endorsementRepository.countBySkillId(10L)).thenReturn(2L);
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(1L);
        when(skillRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Skill result = skillService.upgradeSkillLevelIfEligible(10L);

        assertThat(result.getLevel()).isEqualTo(SkillLevel.INTERMEDIATE);
    }

    @Test
    void upgradeSkillLevel_shouldSetJunior_whenNoEndorsements() {
        skill.setAuthenticityScore(10.0);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(endorsementRepository.countBySkillId(10L)).thenReturn(0L);
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(0L);
        when(skillRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Skill result = skillService.upgradeSkillLevelIfEligible(10L);

        assertThat(result.getLevel()).isEqualTo(SkillLevel.JUNIOR);
    }

    @Test
    void updateExamScore_shouldUpdateAndReturn() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));
        when(skillRepository.save(any())).thenReturn(skill);
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));

        SkillResponse result = skillService.updateExamScore(10L, 100L, 90.0);

        assertThat(result).isNotNull();
        verify(skillAuthenticityService).calculateAuthenticityScore(10L);
    }

    @Test
    void updateExamScore_shouldThrow_whenUnauthorized() {
        when(skillRepository.findById(10L)).thenReturn(Optional.of(skill));

        assertThatThrownBy(() -> skillService.updateExamScore(10L, 999L, 90.0))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("ne vous appartient pas");
    }
}