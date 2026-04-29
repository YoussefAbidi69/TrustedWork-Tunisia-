package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.WorkExperience;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.WorkExperienceRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkExperienceServiceImplTest {

    @Mock private WorkExperienceRepository workExperienceRepository;
    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private ICompletenessService completenessService;

    @InjectMocks
    private WorkExperienceServiceImpl workExperienceService;

    private FreelancerProfile profile;
    private WorkExperience experience;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Backend Developer")
                .build();

        experience = WorkExperience.builder()
                .id(10L)
                .jobTitle("Software Engineer")
                .company("Esprit Corp")
                .location("Tunis")
                .description("Backend development with Java")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .isCurrent(false)
                .profile(profile)
                .build();
    }

    // ─── addWorkExperience ───────────────────────────────────────────────────

    @Test
    void addWorkExperience_shouldSaveAndReturn() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDate(any(), any(), any(), any())).thenReturn(false);
        when(workExperienceRepository.save(any())).thenReturn(experience);

        WorkExperience result = workExperienceService.addWorkExperience(100L, experience);

        assertThat(result).isNotNull();
        assertThat(result.getJobTitle()).isEqualTo("Software Engineer");
        verify(workExperienceRepository).save(any());
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void addWorkExperience_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(99L, experience))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addWorkExperience_shouldThrow_whenJobTitleBlank() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

        WorkExperience badExp = WorkExperience.builder()
                .jobTitle("  ")
                .company("Corp")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 1, 1))
                .isCurrent(false)
                .build();

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(100L, badExp))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("Titre du poste");
    }

    @Test
    void addWorkExperience_shouldThrow_whenCompanyBlank() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

        WorkExperience badExp = WorkExperience.builder()
                .jobTitle("Dev")
                .company("")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 1, 1))
                .isCurrent(false)
                .build();

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(100L, badExp))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("Entreprise");
    }

    @Test
    void addWorkExperience_shouldThrow_whenStartDateInFuture() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

        WorkExperience futureExp = WorkExperience.builder()
                .jobTitle("Dev")
                .company("Corp")
                .startDate(LocalDate.now().plusMonths(1))
                .isCurrent(true)
                .build();

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(100L, futureExp))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("début invalide");
    }

    @Test
    void addWorkExperience_shouldThrow_whenEndDateBeforeStartDate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

        WorkExperience badDates = WorkExperience.builder()
                .jobTitle("Dev")
                .company("Corp")
                .startDate(LocalDate.of(2023, 6, 1))
                .endDate(LocalDate.of(2022, 1, 1))
                .isCurrent(false)
                .build();

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(100L, badDates))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("incohérentes");
    }

    @Test
    void addWorkExperience_shouldThrow_whenDuplicate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDate(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(100L, experience))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("déjà existante");
    }

    @Test
    void addWorkExperience_isCurrent_shouldNullifyEndDate() {
        WorkExperience currentExp = WorkExperience.builder()
                .jobTitle("Dev")
                .company("Corp")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2024, 1, 1))
                .isCurrent(true)
                .build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDate(any(), any(), any(), any())).thenReturn(false);
        when(workExperienceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkExperience result = workExperienceService.addWorkExperience(100L, currentExp);

        assertThat(result.getEndDate()).isNull();
    }

    // ─── getMyWorkExperiences ────────────────────────────────────────────────

    @Test
    void getMyWorkExperiences_shouldReturnList() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByProfileIdOrderByIsCurrentDescStartDateDesc(1L)).thenReturn(List.of(experience));

        List<WorkExperience> result = workExperienceService.getMyWorkExperiences(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobTitle()).isEqualTo("Software Engineer");
    }

    // ─── getWorkExperienceById ───────────────────────────────────────────────

    @Test
    void getWorkExperienceById_shouldReturn() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(experience));

        WorkExperience result = workExperienceService.getWorkExperienceById(10L, 100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getWorkExperienceById_shouldThrow_whenNotFound() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workExperienceService.getWorkExperienceById(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("introuvable");
    }

    // ─── deleteWorkExperience ────────────────────────────────────────────────

    @Test
    void deleteWorkExperience_shouldDeleteSuccessfully() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(experience));

        workExperienceService.deleteWorkExperience(10L, 100L);

        verify(workExperienceRepository).delete(experience);
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void deleteWorkExperience_shouldThrow_whenNotFound() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workExperienceService.deleteWorkExperience(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getTotalExperienceInMonths ──────────────────────────────────────────

    @Test
    void getTotalExperienceInMonths_shouldCalculateCorrectly() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByProfileIdOrderByIsCurrentDescStartDateDesc(1L)).thenReturn(List.of(experience));

        Long months = workExperienceService.getTotalExperienceInMonths(100L);

        // Jan 2022 → Dec 2023 = 23 months
        assertThat(months).isEqualTo(23L);
    }

    @Test
    void getTotalExperienceInMonths_shouldReturn0_whenNoExperiences() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByProfileIdOrderByIsCurrentDescStartDateDesc(1L)).thenReturn(List.of());

        Long months = workExperienceService.getTotalExperienceInMonths(100L);

        assertThat(months).isZero();
    }

    @Test
    void getTotalExperienceInMonths_shouldUseToday_whenEndDateNull() {
        WorkExperience currentJob = WorkExperience.builder()
                .id(20L)
                .jobTitle("Dev")
                .company("Corp")
                .startDate(LocalDate.now().minusMonths(6))
                .endDate(null)
                .isCurrent(true)
                .profile(profile)
                .build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByProfileIdOrderByIsCurrentDescStartDateDesc(1L))
                .thenReturn(List.of(currentJob));

        Long months = workExperienceService.getTotalExperienceInMonths(100L);

        assertThat(months).isGreaterThanOrEqualTo(5L);
    }

    // ─── validateDates — endDate in future ──────────────────────────────────

    @Test
    void addWorkExperience_shouldThrow_whenEndDateInFuture() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));

        WorkExperience futureEndDate = WorkExperience.builder()
                .jobTitle("Dev")
                .company("Corp")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.now().plusMonths(3))
                .isCurrent(false)
                .build();

        assertThatThrownBy(() -> workExperienceService.addWorkExperience(100L, futureEndDate))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("fin invalide");
    }

    // ─── updateWorkExperience ────────────────────────────────────────────────

    @Test
    void updateWorkExperience_shouldUpdateSuccessfully() {
        WorkExperience updates = WorkExperience.builder()
                .jobTitle("Senior Engineer")
                .company("Esprit Corp")
                .location("Ariana")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .isCurrent(false)
                .build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(experience));
        when(workExperienceRepository.existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDateAndIdNot(
                any(), any(), any(), any(), any())).thenReturn(false);
        when(workExperienceRepository.save(any())).thenReturn(experience);

        WorkExperience result = workExperienceService.updateWorkExperience(10L, 100L, updates);

        assertThat(result).isNotNull();
        verify(workExperienceRepository).save(any());
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void updateWorkExperience_shouldThrow_whenNotFound() {
        WorkExperience updates = WorkExperience.builder()
                .jobTitle("Dev")
                .company("Corp")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 1, 1))
                .isCurrent(false)
                .build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workExperienceService.updateWorkExperience(99L, 100L, updates))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateWorkExperience_shouldThrow_whenDuplicate() {
        WorkExperience updates = WorkExperience.builder()
                .jobTitle("Software Engineer")
                .company("Esprit Corp")
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .isCurrent(false)
                .build();

        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(workExperienceRepository.findByIdAndProfileId(10L, 1L)).thenReturn(Optional.of(experience));
        when(workExperienceRepository.existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDateAndIdNot(
                any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> workExperienceService.updateWorkExperience(10L, 100L, updates))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("déjà existante");
    }
}
