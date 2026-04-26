package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @InjectMocks
    private ExcelExportService service;

    @Mock
    private FreelancerProfileRepository repository;

    @Test
    void shouldGenerateExcelFile() throws Exception {
        FreelancerProfile p = new FreelancerProfile();
        p.setId(1L);
        p.setUserId(10L);
        p.setHeadline("Dev");
        p.setRegion("Tunis");
        p.setHourlyRate(50.0);
        p.setRiskScore(10);
        p.setSuspended(false);

        when(repository.findAll()).thenReturn(List.of(p));

        byte[] file = service.generateProfilesExcel();

        assertNotNull(file);
        assertTrue(file.length > 0);
    }

    @Test
    void shouldHandleEmptyProfiles() throws Exception {
        when(repository.findAll()).thenReturn(List.of());

        byte[] file = service.generateProfilesExcel();

        assertNotNull(file);
        assertTrue(file.length > 0);
    }
}