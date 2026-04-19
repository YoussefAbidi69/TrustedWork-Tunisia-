package tn.esprit.userservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillCoverageAnalysisResponseDto {

    private Long id;
    private Long agencyId;

    private String coveredSkills;
    private String missingSkills;

    private Float coveragePercentage;
    private LocalDateTime analyzedAt;
}