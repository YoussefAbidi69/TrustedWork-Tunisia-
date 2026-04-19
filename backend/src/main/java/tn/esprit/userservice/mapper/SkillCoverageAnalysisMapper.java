package tn.esprit.userservice.mapper;


import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.SkillCoverageAnalysisRequestDto;
import tn.esprit.userservice.dto.SkillCoverageAnalysisResponseDto;
import tn.esprit.userservice.entity.SkillCoverageAnalysis;

@Component
public class SkillCoverageAnalysisMapper {

    public SkillCoverageAnalysis toEntity(SkillCoverageAnalysisRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return SkillCoverageAnalysis.builder()
                .coveredSkills(dto.getCoveredSkills())
                .missingSkills(dto.getMissingSkills())
                .build();
    }

    public SkillCoverageAnalysisResponseDto toResponseDto(SkillCoverageAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        return SkillCoverageAnalysisResponseDto.builder()
                .id(analysis.getId())
                .agencyId(analysis.getAgency().getId())
                .coveredSkills(analysis.getCoveredSkills())
                .missingSkills(analysis.getMissingSkills())
                .coveragePercentage(analysis.getCoveragePercentage())
                .analyzedAt(analysis.getAnalyzedAt())
                .build();
    }
}