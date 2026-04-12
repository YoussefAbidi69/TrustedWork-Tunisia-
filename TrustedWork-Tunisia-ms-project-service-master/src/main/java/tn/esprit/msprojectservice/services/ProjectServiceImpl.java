package tn.esprit.msprojectservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.entities.Project;
import tn.esprit.msprojectservice.entities.ProjectStatus;
import tn.esprit.msprojectservice.repositories.IProjectRepository;
import tn.esprit.msprojectservice.services.IProjectService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements IProjectService {

    @Autowired
    private IProjectRepository projectRepository;

    @Override
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        Project project = ProjectDTO.toEntity(projectDTO);
        Project saved = projectRepository.save(project);
        return ProjectDTO.fromEntity(saved);
    }

    @Override
    public ProjectDTO getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + id));
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
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(ProjectDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + id));

        existing.setTitle(projectDTO.getTitle());
        existing.setDescription(projectDTO.getDescription());
        existing.setStartDate(projectDTO.getStartDate());
        existing.setEndDate(projectDTO.getEndDate());
        existing.setBudget(projectDTO.getBudget());

        Project updated = projectRepository.save(existing);
        return ProjectDTO.fromEntity(updated);
    }

    @Override
    public ProjectDTO updateProjectStatus(Long id, ProjectStatus status) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + id));

        existing.setStatus(status);
        Project updated = projectRepository.save(existing);
        return ProjectDTO.fromEntity(updated);
    }

    @Override
    public void deleteProject(Long id) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + id));
        projectRepository.delete(existing);
    }
}