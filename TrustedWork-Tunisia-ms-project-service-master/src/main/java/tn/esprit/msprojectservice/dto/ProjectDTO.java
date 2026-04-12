package tn.esprit.msprojectservice.dto;
import tn.esprit.msprojectservice.entities.Project;
import lombok.*;
import tn.esprit.msprojectservice.entities.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {

    private Long id;
    private String title;
    private String description;
    private ProjectStatus status;
    private Long contractId;
    private Long clientId;
    private Long freelancerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int completionRate;
    private Double budget;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Mapping manuel Entity -> DTO ---
    public static ProjectDTO fromEntity(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .contractId(project.getContractId())
                .clientId(project.getClientId())
                .freelancerId(project.getFreelancerId())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .completionRate(project.getCompletionRate())
                .budget(project.getBudget())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    // --- Mapping manuel DTO -> Entity ---
    public static Project toEntity(ProjectDTO dto) {
        return Project.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .contractId(dto.getContractId())
                .clientId(dto.getClientId())
                .freelancerId(dto.getFreelancerId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .completionRate(dto.getCompletionRate())
                .budget(dto.getBudget())
                .build();
    }
}