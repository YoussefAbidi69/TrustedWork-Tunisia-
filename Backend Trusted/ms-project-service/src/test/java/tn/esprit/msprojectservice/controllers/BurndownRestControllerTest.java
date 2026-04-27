package tn.esprit.msprojectservice.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.msprojectservice.dto.BurndownChartDTO;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.services.IBurndownService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BurndownRestController — Tests unitaires")
class BurndownRestControllerTest {

    @Mock private IBurndownService burndownService;

    @InjectMocks
    private BurndownRestController controller;

    @Test
    @DisplayName("getBurndownChart — retourne HTTP 200 avec le DTO burndown")
    void getBurndownChart_existingProject_returns200WithDTO() {
        BurndownChartDTO expected = BurndownChartDTO.builder()
                .projectId(1L)
                .projectTitle("Projet Test")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .totalTaches(5)
                .courbeIdeale(List.of())
                .courbeReelle(List.of())
                .tachesRestantesAujourdhui(3)
                .tachesCompletees(2)
                .retardEstimeJours(0)
                .velociteJournaliere(0.2)
                .dateLivraisonProjetee(LocalDate.now().plusDays(15))
                .statutBurndown("DANS_LES_DELAIS")
                .analyse("🟡 Le projet avance conformément.")
                .build();

        when(burndownService.getBurndownChart(1L)).thenReturn(expected);

        ResponseEntity<BurndownChartDTO> response = controller.getBurndownChart(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProjectId()).isEqualTo(1L);
        assertThat(response.getBody().getProjectTitle()).isEqualTo("Projet Test");
        assertThat(response.getBody().getStatutBurndown()).isEqualTo("DANS_LES_DELAIS");
        assertThat(response.getBody().getTotalTaches()).isEqualTo(5);
        verify(burndownService).getBurndownChart(1L);
    }

    @Test
    @DisplayName("getBurndownChart — délègue au service sans transformation")
    void getBurndownChart_delegatesToService() {
        BurndownChartDTO dto = BurndownChartDTO.builder().projectId(42L).build();
        when(burndownService.getBurndownChart(42L)).thenReturn(dto);

        ResponseEntity<BurndownChartDTO> response = controller.getBurndownChart(42L);

        assertThat(response.getBody()).isSameAs(dto);
        verify(burndownService, times(1)).getBurndownChart(42L);
    }

    @Test
    @DisplayName("getBurndownChart — propage EntityNotFoundException du service")
    void getBurndownChart_serviceThrows_propagatesException() {
        when(burndownService.getBurndownChart(99L))
                .thenThrow(new EntityNotFoundException("Projet introuvable : 99"));

        assertThatThrownBy(() -> controller.getBurndownChart(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
