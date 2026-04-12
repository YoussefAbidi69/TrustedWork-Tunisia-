package tn.esprit.msprojectservice.services;


import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.entities.ProjectStatus;

import java.util.List;

public interface IProjectService {

    ProjectDTO createProject(ProjectDTO projectDTO);

    ProjectDTO getProjectById(Long id);

    ProjectDTO getProjectByContractId(Long contractId);

    List<ProjectDTO> getProjectsByUserId(Long userId);

    List<ProjectDTO> getAllProjects();

    ProjectDTO updateProject(Long id, ProjectDTO projectDTO);

    ProjectDTO updateProjectStatus(Long id, ProjectStatus status);

    void deleteProject(Long id);
}