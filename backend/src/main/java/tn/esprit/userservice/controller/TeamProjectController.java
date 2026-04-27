package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/agencies")
@RequiredArgsConstructor
public class TeamProjectController {

    private final ITeamProjectServices teamProjectService;
    private final TeamProjectMapper teamProjectMapper;

    // GET /agencies/:id/projects
    @GetMapping("/{agencyId}/projects")
    public List<TeamProjectResponseDto> getProjectsByAgency(@PathVariable Long agencyId) {
        return teamProjectService.getProjectsByAgency(agencyId)
                .stream()
                .map(teamProjectMapper::toResponseDto)
                .toList();
    }

    // GET /agencies/:id/projects/:projectId
    @GetMapping("/{agencyId}/projects/{projectId}")
    public TeamProjectResponseDto getProjectById(@PathVariable Long agencyId, @PathVariable Long projectId) {
        // Technically projectId uniquely identifies it, but we follow the route requested
        TeamProject project = teamProjectService.getProjectById(projectId);
        return teamProjectMapper.toResponseDto(project);
    }

    // POST /agencies/:id/projects
    @PostMapping("/{agencyId}/projects")
    public TeamProjectResponseDto createProject(
            @PathVariable Long agencyId,
            @RequestBody TeamProjectRequestDto dto
    ) {
        // Assume assignedMembers are assigned at creation if present in dto
        TeamProject project = teamProjectMapper.toEntity(dto);
        TeamProject saved = teamProjectService.createProject(agencyId, dto.getCreatorMemberId(), project);
        
        if (dto.getAssignedMembers() != null && !dto.getAssignedMembers().isEmpty()) {
            saved = teamProjectService.assignMembers(saved.getId(), dto.getAssignedMembers(), dto.getCreatorMemberId());
        }
        
        return teamProjectMapper.toResponseDto(saved);
    }

    // PATCH /agencies/:id/projects/:projectId
    @PatchMapping("/{agencyId}/projects/{projectId}")
    public TeamProjectResponseDto updateProject(
            @PathVariable Long agencyId,
            @PathVariable Long projectId,
            @RequestBody TeamProjectUpdateDto dto
    ) {
        TeamProject existing = teamProjectService.getProjectById(projectId);
        teamProjectMapper.updateEntityFromDto(dto, existing);
        TeamProject updated = teamProjectService.updateProject(projectId, existing);
        return teamProjectMapper.toResponseDto(updated);
    }

    // DELETE /agencies/:id/projects/:projectId
    @DeleteMapping("/{agencyId}/projects/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable Long agencyId, @PathVariable Long projectId) {
        teamProjectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    // POST /agencies/:id/projects/:projectId/assign
    @PostMapping("/{agencyId}/projects/{projectId}/assign")
    public TeamProjectResponseDto assignMembers(
            @PathVariable Long agencyId,
            @PathVariable Long projectId,
            @RequestParam Long requesterId,
            @RequestBody java.util.Map<String, List<Long>> body
    ) {
        List<Long> memberIds = body.get("memberIds");
        TeamProject updated = teamProjectService.assignMembers(projectId, memberIds, requesterId);
        return teamProjectMapper.toResponseDto(updated);
    }

    // DELETE /agencies/:id/projects/:projectId/assign/:userId
    @DeleteMapping("/{agencyId}/projects/{projectId}/assign/{memberId}")
    public TeamProjectResponseDto removeMember(
            @PathVariable Long agencyId,
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestParam Long requesterId
    ) {
        TeamProject updated = teamProjectService.removeMember(projectId, memberId, requesterId);
        return teamProjectMapper.toResponseDto(updated);
    }
}