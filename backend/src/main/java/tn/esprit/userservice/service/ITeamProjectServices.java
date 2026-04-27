package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.ProjectStatus;
import tn.esprit.userservice.entity.TeamProject;

import java.util.List;

public interface ITeamProjectServices {

    TeamProject createProject(Long agencyId, Long creatorMemberId, TeamProject project); // Added creatorMemberId

    List<TeamProject> getProjectsByAgency(Long agencyId);

    List<TeamProject> getActiveProjects(Long agencyId);

    List<TeamProject> getProjectsByStatus(Long agencyId, ProjectStatus status);

    TeamProject getProjectById(Long id);

    void deleteProject(Long id);

    TeamProject updateProject(Long projectId, TeamProject updatedProject);

    TeamProject updateProjectProgress(Long projectId);
    
    TeamProject assignMembers(Long projectId, java.util.List<Long> memberIds, Long requesterId);
    TeamProject removeMember(Long projectId, Long memberId, Long requesterId);
}