package tn.esprit.msprojectservice.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.dto.TaskSuggestionResponseDTO;
import tn.esprit.msprojectservice.services.IProjectService;
import tn.esprit.msprojectservice.services.TaskSuggestionService;
import tn.esprit.msprojectservice.services.TaskSuggestionService.TaskSuggestionDTO;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskSuggestionController — Tests unitaires")
class TaskSuggestionControllerTest {

    @Mock private TaskSuggestionService suggestionService;
    @Mock private IProjectService       projectService;

    @InjectMocks
    private TaskSuggestionController controller;

    private ProjectDTO sampleProjectDTO;

    @BeforeEach
    void setUp() {
        sampleProjectDTO = ProjectDTO.builder()
                .id(1L)
                .title("TuniShop E-Commerce Platform")
                .startDate(LocalDate.of(2026, 5, 1))
                .build();
    }

    // ─── suggererTaches ───────────────────────────────────────────────────────

    @Test
    @DisplayName("suggererTaches — retourne HTTP 200 avec les suggestions")
    void suggererTaches_withSuggestions_returns200() {
        LocalDate deadline1 = LocalDate.of(2026, 5, 8);
        LocalDate deadline2 = LocalDate.of(2026, 5, 15);

        List<TaskSuggestionDTO> suggestions = List.of(
                new TaskSuggestionDTO("Maquette UI/UX", "Wireframes Figma", 1, 16, deadline1, "E_COMMERCE"),
                new TaskSuggestionDTO("Setup Backend", "Architecture API", 2, 24, deadline2, "E_COMMERCE")
        );

        when(projectService.getProjectById(1L)).thenReturn(sampleProjectDTO);
        when(suggestionService.suggererTaches("TuniShop E-Commerce Platform",
                LocalDate.of(2026, 5, 1))).thenReturn(suggestions);

        ResponseEntity<TaskSuggestionResponseDTO> response = controller.suggererTaches(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProjectId()).isEqualTo(1L);
        assertThat(response.getBody().getProjectTitle()).isEqualTo("TuniShop E-Commerce Platform");
        assertThat(response.getBody().getDetectedType()).isEqualTo("E_COMMERCE");
        assertThat(response.getBody().getSuggestedTasks()).hasSize(2);
    }

    @Test
    @DisplayName("suggererTaches — premier élément contient les bonnes données")
    void suggererTaches_firstSuggestion_hasCorrectData() {
        LocalDate deadline = LocalDate.of(2026, 5, 8);
        List<TaskSuggestionDTO> suggestions = List.of(
                new TaskSuggestionDTO("Maquette UI/UX", "Wireframes Figma", 1, 16, deadline, "E_COMMERCE")
        );

        when(projectService.getProjectById(1L)).thenReturn(sampleProjectDTO);
        when(suggestionService.suggererTaches(anyString(), any())).thenReturn(suggestions);

        ResponseEntity<TaskSuggestionResponseDTO> response = controller.suggererTaches(1L);

        TaskSuggestionResponseDTO.TacheSuggereDTO firstTask =
                response.getBody().getSuggestedTasks().get(0);
        assertThat(firstTask.getTitle()).isEqualTo("Maquette UI/UX");
        assertThat(firstTask.getDescription()).isEqualTo("Wireframes Figma");
        assertThat(firstTask.getOrder()).isEqualTo(1);
        assertThat(firstTask.getEstimatedHours()).isEqualTo(16);
        assertThat(firstTask.getDeadline()).isEqualTo(deadline);
    }

    @Test
    @DisplayName("suggererTaches — liste vide → detectedType = 'INCONNU'")
    void suggererTaches_emptySuggestions_returnsInconnuType() {
        when(projectService.getProjectById(1L)).thenReturn(sampleProjectDTO);
        when(suggestionService.suggererTaches(anyString(), any())).thenReturn(List.of());

        ResponseEntity<TaskSuggestionResponseDTO> response = controller.suggererTaches(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getDetectedType()).isEqualTo("INCONNU");
        assertThat(response.getBody().getSuggestedTasks()).isEmpty();
    }

    @Test
    @DisplayName("suggererTaches — startDate null → LocalDate.now() utilisé")
    void suggererTaches_nullStartDate_usesCurrentDate() {
        ProjectDTO projectWithoutStartDate = ProjectDTO.builder()
                .id(2L).title("Projet Sans Date").startDate(null).build();

        when(projectService.getProjectById(2L)).thenReturn(projectWithoutStartDate);
        when(suggestionService.suggererTaches(anyString(), any())).thenReturn(List.of());

        ResponseEntity<TaskSuggestionResponseDTO> response = controller.suggererTaches(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // When startDate is null, controller uses LocalDate.now()
        verify(suggestionService).suggererTaches(eq("Projet Sans Date"), any(LocalDate.class));
    }

    @Test
    @DisplayName("suggererTaches — appelle les bons services avec les bons paramètres")
    void suggererTaches_callsServicesWithCorrectParams() {
        when(projectService.getProjectById(1L)).thenReturn(sampleProjectDTO);
        when(suggestionService.suggererTaches(anyString(), any())).thenReturn(List.of());

        controller.suggererTaches(1L);

        verify(projectService).getProjectById(1L);
        verify(suggestionService).suggererTaches(
                "TuniShop E-Commerce Platform",
                LocalDate.of(2026, 5, 1));
    }

    @Test
    @DisplayName("suggererTaches — réponse contient projectId et projectTitle corrects")
    void suggererTaches_responseContainsProjectMetadata() {
        when(projectService.getProjectById(1L)).thenReturn(sampleProjectDTO);
        when(suggestionService.suggererTaches(anyString(), any())).thenReturn(List.of());

        ResponseEntity<TaskSuggestionResponseDTO> response = controller.suggererTaches(1L);

        assertThat(response.getBody().getProjectId()).isEqualTo(1L);
        assertThat(response.getBody().getProjectTitle()).isEqualTo("TuniShop E-Commerce Platform");
    }
}
