package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.TeamProjectRequestDto;
import tn.esprit.userservice.dto.TeamProjectResponseDto;
import tn.esprit.userservice.dto.TeamProjectUpdateDto;
import tn.esprit.userservice.entity.TeamProject;

import java.math.BigDecimal;

@Component
public class TeamProjectMapper {

    public TeamProject toEntity(TeamProjectRequestDto dto) {
        if (dto == null) {
            return null;
        }

        // Note: agency and createdByMember must be linked in the service layer
        return TeamProject.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .budget(dto.getBudget() != null ? BigDecimal.valueOf(dto.getBudget()) : null)
                .status(dto.getStatus())
                .progress(dto.getProgress())
                .active(dto.getActive())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
    }

    public TeamProjectResponseDto toResponseDto(TeamProject project) {
        if (project == null) {
            return null;
        }

        return TeamProjectResponseDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .budget(project.getBudget() != null ? project.getBudget().floatValue() : null)
                .status(project.getStatus())
                .progress(project.getProgress())
                .active(project.getActive())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .creatorMemberId(project.getCreatedByMember() != null ? project.getCreatedByMember().getId() : null)
                .agencyId(project.getAgency() != null ? project.getAgency().getId() : null)
                .build();
    }

    public void updateEntityFromDto(TeamProjectUpdateDto dto, TeamProject project) {
        if (dto == null || project == null) {
            return;
        }

        if (dto.getTitle() != null) {
            project.setTitle(dto.getTitle());
        }

        if (dto.getDescription() != null) {
            project.setDescription(dto.getDescription());
        }

        if (dto.getBudget() != null) {
            project.setBudget(BigDecimal.valueOf(dto.getBudget()));
        }

        if (dto.getStatus() != null) {
            project.setStatus(dto.getStatus());
        }

        if (dto.getProgress() != null) {
            project.setProgress(dto.getProgress());
        }

        if (dto.getActive() != null) {
            project.setActive(dto.getActive());
        }

        if (dto.getStartDate() != null) {
            project.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            project.setEndDate(dto.getEndDate());
        }
    }
}