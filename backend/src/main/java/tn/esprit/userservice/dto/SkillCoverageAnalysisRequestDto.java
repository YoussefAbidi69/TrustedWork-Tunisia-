package tn.esprit.userservice.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCoverageAnalysisRequestDto {

    private String coveredSkills;
    private String missingSkills;
}