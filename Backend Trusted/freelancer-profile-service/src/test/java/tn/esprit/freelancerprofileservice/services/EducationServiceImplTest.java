package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.dto.request.UpdateEducationRequest;
import tn.esprit.freelancerprofileservice.entities.Education;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.exceptions.UnauthorizedActionException;
import tn.esprit.freelancerprofileservice.repositories.EducationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EducationServiceImplTest {

    @Mock private EducationRepository educationRepository;
    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private ICompletenessService completenessService;

    @InjectMocks
    private EducationServiceImpl educationService;

    private FreelancerProfile profile;
    private Education education;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Full Stack Developer")
                .build();

        education = Education.builder()
                .id(10L)
                .degree("Master Informatique")
                .institution("ESPRIT")
                .fieldOfStudy("Cloud Computing")
                .graduationYear(2024)
                .profile(profile)
                .build();
    }

    // ─── addEducation ────────────────────────────────────────────────────────

    @Test
    void addEducation_shouldSaveAndReturn() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(educationRepository.existsByDegreeIgnoreCaseAndInstitutionIgnoreCaseAndProfileId(any(), any(), any())).thenReturn(false);
        when(educationRepository.save(any())).thenReturn(education);

        Education result = educationService.addEducation(100L, education);

        assertThat(result).isNotNull();
        assertThat(result.getDegree()).isEqualTo("Master Informatique");
        assertThat(result.getInstitution()).isEqualTo("ESPRIT");
        verify(educationRepository).save(any());
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void addEducation_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> educationService.addEducation(99L, education))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void addEducation_shouldThrow_whenDuplicate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(educationRepository.existsByDegreeIgnoreCaseAndInstitutionIgnoreCaseAndProfileId(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> educationService.addEducation(100L, education))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("déjà déclaré");
    }

    // ─── updateEducation ─────────────────────────────────────────────────────

    @Test
    void updateEducation_shouldUpdateAllFields() {
        UpdateEducationRequest request = new UpdateEducationRequest();
        request.setDegree("Licence Informatique");
        request.setInstitution("INSAT");
        request.setFieldOfStudy("DevOps");
        request.setGraduationYear(2022);

        when(educationRepository.findById(10L)).thenReturn(Optional.of(education));
        when(educationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Education result = educationService.updateEducation(10L, 100L, request);

        assertThat(result.getDegree()).isEqualTo("Licence Informatique");
        assertThat(result.getInstitution()).isEqualTo("INSAT");
        assertThat(result.getFieldOfStudy()).isEqualTo("DevOps");
        assertThat(result.getGraduationYear()).isEqualTo(2022);
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void updateEducation_shouldIgnoreBlankFields() {
        UpdateEducationRequest request = new UpdateEducationRequest();
        request.setDegree("  ");
        request.setInstitution("");

        when(educationRepository.findById(10L)).thenReturn(Optional.of(education));
        when(educationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Education result = educationService.updateEducation(10L, 100L, request);

        // Original values should stay unchanged
        assertThat(result.getDegree()).isEqualTo("Master Informatique");
        assertThat(result.getInstitution()).isEqualTo("ESPRIT");
    }

    @Test
    void updateEducation_shouldThrow_whenNotFound() {
        when(educationRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateEducationRequest request = new UpdateEducationRequest();

        assertThatThrownBy(() -> educationService.updateEducation(99L, 100L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateEducation_shouldThrow_whenUnauthorized() {
        when(educationRepository.findById(10L)).thenReturn(Optional.of(education));

        UpdateEducationRequest request = new UpdateEducationRequest();

        assertThatThrownBy(() -> educationService.updateEducation(10L, 999L, request))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    // ─── getMyEducations ─────────────────────────────────────────────────────

    @Test
    void getMyEducations_shouldReturnList() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(educationRepository.findByProfileIdOrderByGraduationYearDesc(1L)).thenReturn(List.of(education));

        List<Education> result = educationService.getMyEducations(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDegree()).isEqualTo("Master Informatique");
    }

    @Test
    void getMyEducations_shouldReturnEmpty_whenNone() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(educationRepository.findByProfileIdOrderByGraduationYearDesc(1L)).thenReturn(List.of());

        List<Education> result = educationService.getMyEducations(100L);

        assertThat(result).isEmpty();
    }

    // ─── deleteEducation ─────────────────────────────────────────────────────

    @Test
    void deleteEducation_shouldDeleteSuccessfully() {
        when(educationRepository.findById(10L)).thenReturn(Optional.of(education));

        educationService.deleteEducation(10L, 100L);

        verify(educationRepository).delete(education);
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void deleteEducation_shouldThrow_whenUnauthorized() {
        when(educationRepository.findById(10L)).thenReturn(Optional.of(education));

        assertThatThrownBy(() -> educationService.deleteEducation(10L, 999L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void deleteEducation_shouldThrow_whenNotFound() {
        when(educationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> educationService.deleteEducation(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
