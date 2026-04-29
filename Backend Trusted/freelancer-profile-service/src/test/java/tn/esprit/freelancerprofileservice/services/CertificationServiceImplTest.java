package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.dto.request.UpdateCertificationRequest;
import tn.esprit.freelancerprofileservice.entities.Certification;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.CertificationType;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.InvalidDataException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.exceptions.UnauthorizedActionException;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificationServiceImplTest {

    @Mock private CertificationRepository certificationRepository;
    @Mock private FreelancerProfileRepository profileRepository;
    @Mock private ICompletenessService completenessService;

    @InjectMocks
    private CertificationServiceImpl certificationService;

    private FreelancerProfile profile;
    private Certification certification;

    @BeforeEach
    void setUp() {
        profile = FreelancerProfile.builder()
                .id(1L)
                .userId(100L)
                .headline("Java Developer")
                .build();

        certification = Certification.builder()
                .id(10L)
                .title("AWS Solutions Architect")
                .issuer("Amazon")
                .type(CertificationType.EXTERNAL)
                .issueDate(LocalDate.of(2023, 1, 1))
                .expiryDate(LocalDate.of(2026, 1, 1))
                .certificateUrl("https://aws.amazon.com/cert/123")
                .isExpired(false)
                .profile(profile)
                .build();
    }

    // ─── addCertification ────────────────────────────────────────────────────

    @Test
    void addCertification_shouldSaveAndReturn() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(certificationRepository.countByProfileId(1L)).thenReturn(0L);
        when(certificationRepository.existsByProfileIdAndTitleIgnoreCaseAndIssuerIgnoreCase(any(), any(), any())).thenReturn(false);
        when(certificationRepository.save(any())).thenReturn(certification);

        Certification result = certificationService.addCertification(100L, certification);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("AWS Solutions Architect");
        verify(certificationRepository).save(any());
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void addCertification_shouldThrow_whenProfileNotFound() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificationService.addCertification(99L, certification))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addCertification_shouldThrow_whenLimitReached() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(certificationRepository.countByProfileId(1L)).thenReturn(20L);

        assertThatThrownBy(() -> certificationService.addCertification(100L, certification))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("Limite atteinte");
    }

    @Test
    void addCertification_shouldThrow_whenDuplicate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(certificationRepository.countByProfileId(1L)).thenReturn(0L);
        when(certificationRepository.existsByProfileIdAndTitleIgnoreCaseAndIssuerIgnoreCase(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> certificationService.addCertification(100L, certification))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void addCertification_shouldThrow_whenIssueDateAfterExpiryDate() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(certificationRepository.countByProfileId(1L)).thenReturn(0L);
        when(certificationRepository.existsByProfileIdAndTitleIgnoreCaseAndIssuerIgnoreCase(any(), any(), any())).thenReturn(false);

        Certification badDates = Certification.builder()
                .title("Bad Cert")
                .issuer("Issuer")
                .issueDate(LocalDate.of(2025, 6, 1))
                .expiryDate(LocalDate.of(2024, 1, 1))
                .build();

        assertThatThrownBy(() -> certificationService.addCertification(100L, badDates))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("date d'émission");
    }

    // ─── updateCertification ─────────────────────────────────────────────────

    @Test
    void updateCertification_shouldUpdateSuccessfully() {
        UpdateCertificationRequest request = new UpdateCertificationRequest();
        request.setTitle("Updated Title");
        request.setIssuer("Updated Issuer");

        when(certificationRepository.findById(10L)).thenReturn(Optional.of(certification));
        when(certificationRepository.existsByProfileIdAndTitleIgnoreCaseAndIssuerIgnoreCaseAndIdNot(any(), any(), any(), any())).thenReturn(false);
        when(certificationRepository.save(any())).thenReturn(certification);

        Certification result = certificationService.updateCertification(10L, 100L, request);

        assertThat(result).isNotNull();
        verify(certificationRepository).save(any());
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void updateCertification_shouldThrow_whenNotFound() {
        when(certificationRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateCertificationRequest request = new UpdateCertificationRequest();

        assertThatThrownBy(() -> certificationService.updateCertification(99L, 100L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCertification_shouldThrow_whenUnauthorized() {
        when(certificationRepository.findById(10L)).thenReturn(Optional.of(certification));

        UpdateCertificationRequest request = new UpdateCertificationRequest();

        assertThatThrownBy(() -> certificationService.updateCertification(10L, 999L, request))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    // ─── getMyCertifications ─────────────────────────────────────────────────

    @Test
    void getMyCertifications_shouldReturnList() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(certificationRepository.findByProfileIdOrderByIssueDateDesc(1L)).thenReturn(List.of(certification));

        List<Certification> result = certificationService.getMyCertifications(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("AWS Solutions Architect");
    }

    // ─── deleteCertification ─────────────────────────────────────────────────

    @Test
    void deleteCertification_shouldDeleteSuccessfully() {
        when(certificationRepository.findById(10L)).thenReturn(Optional.of(certification));

        certificationService.deleteCertification(10L, 100L);

        verify(certificationRepository).delete(certification);
        verify(completenessService).calculateCompleteness(100L);
    }

    @Test
    void deleteCertification_shouldThrow_whenUnauthorized() {
        when(certificationRepository.findById(10L)).thenReturn(Optional.of(certification));

        assertThatThrownBy(() -> certificationService.deleteCertification(10L, 999L))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void deleteCertification_shouldThrow_whenNotFound() {
        when(certificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificationService.deleteCertification(99L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
