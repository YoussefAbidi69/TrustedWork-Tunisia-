package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.TeamProjectRequestDto;
import tn.esprit.userservice.dto.TeamProjectResponseDto;
import tn.esprit.userservice.dto.TeamProjectUpdateDto;
import tn.esprit.userservice.entity.ProjectStatus;
import tn.esprit.userservice.entity.TeamProject;
import tn.esprit.userservice.mapper.TeamProjectMapper;
import tn.esprit.userservice.service.ITeamProjectServices;

import java.util.List;

@RestController
@RequestMapping("/team-projects")
@RequiredArgsConstructor
public class TeamProjectController {

    private final ITeamProjectServices teamProjectService;
    private final TeamProjectMapper teamProjectMapper;

    // CREATE PROJECT IN AGENCY
    @PostMapping("/agency/{agencyId}")
    public TeamProjectResponseDto createProject(
            @PathVariable Long agencyId,
            @RequestBody TeamProjectRequestDto dto
    ) {
        TeamProject project = teamProjectMapper.toEntity(dto);
        // Explicitly passing creatorMemberId to handles project attribution
        TeamProject saved = teamProjectService.createProject(agencyId, dto.getCreatorMemberId(), project);
        return teamProjectMapper.toResponseDto(saved);
    }

    // GET ALL PROJECTS OF AN AGENCY
    @GetMapping("/agency/{agencyId}")
    public List<TeamProjectResponseDto> getProjectsByAgency(@PathVariable Long agencyId) {
        return teamProjectService.getProjectsByAgency(agencyId)
                .stream()
                .map(teamProjectMapper::toResponseDto)
                .toList();
    }

    // GET ACTIVE PROJECTS OF AN AGENCY
    @GetMapping("/agency/{agencyId}/active")
    public List<TeamProjectResponseDto> getActiveProjects(@PathVariable Long agencyId) {
        return teamProjectService.getActiveProjects(agencyId)
                .stream()
                .map(teamProjectMapper::toResponseDto)
                .toList();
    }

    // GET PROJECTS BY STATUS
    @GetMapping("/agency/{agencyId}/status")
    public List<TeamProjectResponseDto> getProjectsByStatus(
            @PathVariable Long agencyId,
            @RequestParam ProjectStatus status
    ) {
        return teamProjectService.getProjectsByStatus(agencyId, status)
                .stream()
                .map(teamProjectMapper::toResponseDto)
                .toList();
    }

    // GET PROJECT BY ID
    @GetMapping("/{projectId}")
    public TeamProjectResponseDto getProjectById(@PathVariable Long projectId) {
        TeamProject project = teamProjectService.getProjectById(projectId);
        return teamProjectMapper.toResponseDto(project);
    }

    // UPDATE PROJECT
    @PutMapping("/{projectId}")
    public TeamProjectResponseDto updateProject(
            @PathVariable Long projectId,
            @RequestBody TeamProjectUpdateDto dto
    ) {
        TeamProject existing = teamProjectService.getProjectById(projectId);
        teamProjectMapper.updateEntityFromDto(dto, existing);
        TeamProject updated = teamProjectService.updateProject(projectId, existing);
        return teamProjectMapper.toResponseDto(updated);
    }

    // UPDATE PROJECT PROGRESS
    @PutMapping("/{projectId}/progress")
    public TeamProjectResponseDto updateProjectProgress(@PathVariable Long projectId) {
        TeamProject updated = teamProjectService.updateProjectProgress(projectId);
        return teamProjectMapper.toResponseDto(updated);
    }

    // DELETE PROJECT
    @DeleteMapping("/{projectId}")
    public void deleteProject(@PathVariable Long projectId) {
        teamProjectService.deleteProject(projectId);
    }
}