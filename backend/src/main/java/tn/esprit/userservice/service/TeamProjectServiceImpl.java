package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.ITeamProjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamProjectServiceImpl implements ITeamProjectServices {

    private final ITeamProjectRepository teamProjectRepository;
    private final IAgencyRepository agencyRepository;
    private final IAgencyMemberRepository agencyMemberRepository;

    @Override
    public TeamProject createProject(Long agencyId, Long creatorMemberId, TeamProject project) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        AgencyMember creator = agencyMemberRepository.findById(creatorMemberId)
                .orElseThrow(() -> new RuntimeException("Agency Member not found"));

        if (!creator.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Creator must belong to the agency of the project");
        }

        if (creator.getRole() != MemberRole.LEAD) {
            throw new RuntimeException("Only a LEAD can create a team project");
        }

        project.setAgency(agency);
        project.setCreatedByMember(creator);
        project.setStatus(ProjectStatus.PLANNED);
        project.setProgress(0);
        project.setActive(true);

        return teamProjectRepository.save(project);
    }

    @Override
    public List<TeamProject> getProjectsByAgency(Long agencyId) {
        return teamProjectRepository.findByAgencyId(agencyId);
    }

    @Override
    public List<TeamProject> getActiveProjects(Long agencyId) {
        // Updated to use status/active field logic
        return teamProjectRepository.findAll().stream()
                .filter(p -> p.getAgency().getId().equals(agencyId) && p.getActive())
                .toList();
    }

    @Override
    public List<TeamProject> getProjectsByStatus(Long agencyId, ProjectStatus status) {
        return teamProjectRepository.findAll().stream()
                .filter(p -> p.getAgency().getId().equals(agencyId) && p.getStatus() == status)
                .toList();
    }

    @Override
    public TeamProject getProjectById(Long id) {
        return teamProjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @Override
    public void deleteProject(Long id) {
        teamProjectRepository.deleteById(id);
    }

    @Override
    public TeamProject updateProject(Long projectId, TeamProject updatedProject) {
        TeamProject existingProject = teamProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (updatedProject.getTitle() != null) {
            existingProject.setTitle(updatedProject.getTitle());
        }

        if (updatedProject.getDescription() != null) {
            existingProject.setDescription(updatedProject.getDescription());
        }

        if (updatedProject.getBudget() != null) {
            existingProject.setBudget(updatedProject.getBudget());
        }

        if (updatedProject.getStatus() != null) {
            existingProject.setStatus(updatedProject.getStatus());
        }

        if (updatedProject.getProgress() != null) {
            existingProject.setProgress(updatedProject.getProgress());
        }

        if (updatedProject.getActive() != null) {
            existingProject.setActive(updatedProject.getActive());
        }

        if (updatedProject.getStartDate() != null) {
            existingProject.setStartDate(updatedProject.getStartDate());
        }

        if (updatedProject.getEndDate() != null) {
            existingProject.setEndDate(updatedProject.getEndDate());
        }

        return teamProjectRepository.save(existingProject);
    }

    @Override
    public TeamProject updateProjectProgress(Long projectId) {
        TeamProject project = teamProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Task> tasks = project.getTasks();

        if (tasks == null || tasks.isEmpty()) {
            project.setProgress(0);
            return teamProjectRepository.save(project);
        }

        long totalTasks = tasks.size();

        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        int progress = (int) ((completedTasks * 100) / totalTasks);

        project.setProgress(progress);

        return teamProjectRepository.save(project);
    }
}