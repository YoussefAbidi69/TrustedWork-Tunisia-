package tn.esprit.smartjobboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerRoadmapStepDto {
    private Integer id;
    private String title;
    private String description;
    private String difficultyLevel;
    private Integer estimatedWeeks;
    private Integer hoursPerDay;
    private Double incomeBoostThisStep;
    private List<MicroCurriculumDto> microCurriculum;
    private List<String> resources;
    private String portfolioProject;
    private List<String> prerequisiteSkills;
    private List<String> skillsUnlocked;
    private String demandLevel;
    private String color;
}
