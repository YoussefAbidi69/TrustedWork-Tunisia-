package tn.esprit.msprojectservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.ProjectStatus;
import tn.esprit.msprojectservice.feign.UserServiceClient;
import tn.esprit.msprojectservice.feign.dto.UserDTO;
import tn.esprit.msprojectservice.repositories.IProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService {

    private static final String PROJECT_NOT_FOUND = "Projet non trouvé avec l'id : ";

    private final IProjectRepository projectRepository;
    private final UserServiceClient userServiceClient;

    // ==================== CRUD DE BASE ====================

    @Override
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        Project project = ProjectDTO.toEntity(projectDTO);
        Project saved = projectRepository.save(project);
        return ProjectDTO.fromEntity(saved);
    }

    @Override
    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PROJECT_NOT_FOUND + id));
        return ProjectDTO.fromEntity(project);
    }

    @Override
    public ProjectDTO getProjectByContractId(Long contractId) {
        Project project = projectRepository.findByContractId(contractId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé pour le contrat : " + contractId));
        return ProjectDTO.fromEntity(project);
    }

    @Override
    public List<ProjectDTO> getProjectsByUserId(Long userId) {
        return projectRepository.findAllByUserId(userId)
                .stream()
                .map(ProjectDTO::fromEntity)
                .toList();
    }

    @Override
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(ProjectDTO::fromEntity)
                .toList();
    }

    @Override
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PROJECT_NOT_FOUND + id));
        existing.setTitle(projectDTO.getTitle());
        existing.setDescription(projectDTO.getDescription());
        existing.setStartDate(projectDTO.getStartDate());
        existing.setEndDate(projectDTO.getEndDate());
        existing.setBudget(projectDTO.getBudget());
        return ProjectDTO.fromEntity(projectRepository.save(existing));
    }

    @Override
    public ProjectDTO updateProjectStatus(Long id, ProjectStatus status) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PROJECT_NOT_FOUND + id));
        existing.setStatus(status);
        return ProjectDTO.fromEntity(projectRepository.save(existing));
    }

    @Override
    public void deleteProject(Long id) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PROJECT_NOT_FOUND + id));
        projectRepository.delete(existing);
    }

    // ==================== MÉTHODES ENRICHIES ====================

    /**
     * Retourne un projet enrichi avec les données utilisateur (CIN, nom, prénom).
     */
    @Override
    public ProjectDTO getProjectByIdEnriched(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PROJECT_NOT_FOUND + id));
        return enrichProject(project);
    }

    /**
     * Retourne les projets de l'utilisateur connecté, enrichis avec les données utilisateur.
     * Utilisé par GET /api/projects/my
     */
    @Override
    public List<ProjectDTO> getMyProjects(Long userId) {
        return projectRepository.findAllByUserId(userId)
                .stream()
                .map(this::enrichProject)
                .toList();
    }

    // ==================== ENRICHISSEMENT PRIVÉ ====================

    /**
     * Appelle le User Service pour récupérer CIN, nom, prénom, email
     * du client et du freelancer et les injecte dans le DTO.
     */
    private ProjectDTO enrichProject(Project project) {
        ProjectDTO dto = ProjectDTO.fromEntity(project);

        // Infos client
        if (project.getClientId() != null) {
            UserDTO client = userServiceClient.getUserById(project.getClientId());
            if (client != null) {
                dto.setClientCin(client.getCin());
                dto.setClientFirstName(client.getFirstName());
                dto.setClientLastName(client.getLastName());
                dto.setClientEmail(client.getEmail());
            }
        }

        // Infos freelancer
        if (project.getFreelancerId() != null) {
            UserDTO freelancer = userServiceClient.getUserById(project.getFreelancerId());
            if (freelancer != null) {
                dto.setFreelancerCin(freelancer.getCin());
                dto.setFreelancerFirstName(freelancer.getFirstName());
                dto.setFreelancerLastName(freelancer.getLastName());
                dto.setFreelancerEmail(freelancer.getEmail());
            }
        }

        return dto;
    }
}