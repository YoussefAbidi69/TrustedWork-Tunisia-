package tn.esprit.msprojectservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.ProjectStatus;
import tn.esprit.msprojectservice.feign.UserServiceClient;
import tn.esprit.msprojectservice.feign.dto.UserDTO;
import tn.esprit.msprojectservice.repositories.IProjectRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl — Tests unitaires")
class ProjectServiceImplTest {

    @Mock
    private IProjectRepository projectRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project sampleProject;

    @BeforeEach
    void setUp() {
        sampleProject = Project.builder()
                .id(1L)
                .title("TrustedWork Alpha")
                .description("Projet de test")
                .status(ProjectStatus.ACTIVE)
                .contractId(10L)
                .clientId(100L)
                .freelancerId(200L)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .budget(15000.0)
                .completionRate(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createProject
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProject — doit sauvegarder et retourner le DTO")
    void createProject_shouldPersistAndReturnDTO() {
        ProjectDTO dto = buildProjectDTO();
        when(projectRepository.save(any(Project.class))).thenReturn(sampleProject);

        ProjectDTO result = projectService.createProject(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("TrustedWork Alpha");
        verify(projectRepository).save(any(Project.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getProjectById
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectById — retourne le DTO quand le projet existe")
    void getProjectById_shouldReturnDTO_whenExists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        ProjectDTO result = projectService.getProjectById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("TrustedWork Alpha");
    }

    @Test
    @DisplayName("getProjectById — lève RuntimeException quand projet introuvable")
    void getProjectById_shouldThrow_whenNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getProjectByContractId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectByContractId — retourne le projet correspondant")
    void getProjectByContractId_shouldReturnProject() {
        when(projectRepository.findByContractId(10L)).thenReturn(Optional.of(sampleProject));

        ProjectDTO result = projectService.getProjectByContractId(10L);

        assertThat(result.getContractId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getProjectByContractId — lève RuntimeException si contrat inconnu")
    void getProjectByContractId_shouldThrow_whenNotFound() {
        when(projectRepository.findByContractId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectByContractId(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("999");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getProjectsByUserId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectsByUserId — retourne la liste des projets de l'utilisateur")
    void getProjectsByUserId_shouldReturnList() {
        Project p2 = Project.builder().id(2L).title("Projet B")
                .status(ProjectStatus.ACTIVE).completionRate(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(projectRepository.findAllByUserId(100L)).thenReturn(List.of(sampleProject, p2));

        List<ProjectDTO> results = projectService.getProjectsByUserId(100L);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ProjectDTO::getId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("getProjectsByUserId — retourne liste vide si aucun projet")
    void getProjectsByUserId_shouldReturnEmptyList_whenNone() {
        when(projectRepository.findAllByUserId(999L)).thenReturn(List.of());

        List<ProjectDTO> results = projectService.getProjectsByUserId(999L);

        assertThat(results).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllProjects
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllProjects — retourne tous les projets")
    void getAllProjects_shouldReturnAll() {
        when(projectRepository.findAll()).thenReturn(List.of(sampleProject));

        List<ProjectDTO> results = projectService.getAllProjects();

        assertThat(results).hasSize(1);
        verify(projectRepository).findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateProject
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProject — met à jour les champs modifiables")
    void updateProject_shouldUpdateFields() {
        ProjectDTO updateDTO = buildProjectDTO();
        updateDTO.setTitle("Nouveau Titre");
        updateDTO.setBudget(20000.0);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectDTO result = projectService.updateProject(1L, updateDTO);

        assertThat(result.getTitle()).isEqualTo("Nouveau Titre");
        assertThat(result.getBudget()).isEqualTo(20000.0);
        verify(projectRepository).save(sampleProject);
    }

    @Test
    @DisplayName("updateProject — lève RuntimeException si projet non trouvé")
    void updateProject_shouldThrow_whenNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        ProjectDTO dto = buildProjectDTO();

        assertThatThrownBy(() -> projectService.updateProject(99L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(projectRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateProjectStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProjectStatus — change le statut du projet")
    void updateProjectStatus_shouldChangeStatus() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjectDTO result = projectService.updateProjectStatus(1L, ProjectStatus.COMPLETED);

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }

    @Test
    @DisplayName("updateProjectStatus — lève RuntimeException si projet non trouvé")
    void updateProjectStatus_shouldThrow_whenNotFound() {
        when(projectRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProjectStatus(5L, ProjectStatus.ON_HOLD))
                .isInstanceOf(RuntimeException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteProject
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProject — supprime le projet existant")
    void deleteProject_shouldDelete_whenExists() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));

        projectService.deleteProject(1L);

        verify(projectRepository).delete(sampleProject);
    }

    @Test
    @DisplayName("deleteProject — lève RuntimeException si projet non trouvé")
    void deleteProject_shouldThrow_whenNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.deleteProject(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(projectRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getProjectByIdEnriched
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectByIdEnriched — enrichit le projet avec les données client et freelancer")
    void getProjectByIdEnriched_shouldEnrichWithUserData() {
        UserDTO client     = buildUser(100L, "Ahmed",   "Ben Ali");
        UserDTO freelancer = buildUser(200L, "Sami",    "Mrad");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(userServiceClient.getUserById(100L)).thenReturn(client);
        when(userServiceClient.getUserById(200L)).thenReturn(freelancer);

        ProjectDTO result = projectService.getProjectByIdEnriched(1L);

        assertThat(result.getClientFirstName()).isEqualTo("Ahmed");
        assertThat(result.getFreelancerFirstName()).isEqualTo("Sami");
        verify(userServiceClient).getUserById(100L);
        verify(userServiceClient).getUserById(200L);
    }

    @Test
    @DisplayName("getProjectByIdEnriched — tolère l'absence de données utilisateur (null)")
    void getProjectByIdEnriched_shouldTolerate_whenUserServiceReturnsNull() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(sampleProject));
        when(userServiceClient.getUserById(anyLong())).thenReturn(null);

        assertThatCode(() -> projectService.getProjectByIdEnriched(1L))
                .doesNotThrowAnyException();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ProjectDTO buildProjectDTO() {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(1L);
        dto.setTitle("TrustedWork Alpha");
        dto.setDescription("Projet de test");
        dto.setStatus(ProjectStatus.ACTIVE);
        dto.setContractId(10L);
        dto.setClientId(100L);
        dto.setFreelancerId(200L);
        dto.setStartDate(LocalDate.of(2024, 1, 1));
        dto.setEndDate(LocalDate.of(2024, 12, 31));
        dto.setBudget(15000.0);
        return dto;
    }

    private UserDTO buildUser(Long id, String firstName, String lastName) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(firstName.toLowerCase() + "@trustedwork.tn");
        return user;
    }
}
