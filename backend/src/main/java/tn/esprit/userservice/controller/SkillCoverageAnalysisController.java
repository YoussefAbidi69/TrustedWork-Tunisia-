package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.SkillCoverageAnalysisRequestDto;
import tn.esprit.userservice.dto.SkillCoverageAnalysisResponseDto;
import tn.esprit.userservice.entity.SkillCoverageAnalysis;
import tn.esprit.userservice.mapper.SkillCoverageAnalysisMapper;
import tn.esprit.userservice.service.ISkillCoverageAnalysisServices;

import java.util.List;

@RestController
@RequestMapping("/skill-coverage-analyses")
@RequiredArgsConstructor
public class SkillCoverageAnalysisController {

    private final ISkillCoverageAnalysisServices skillCoverageAnalysisService;
    private final SkillCoverageAnalysisMapper skillCoverageAnalysisMapper;

    // CREATE ANALYSIS
    @PostMapping("/agency/{agencyId}")
    public SkillCoverageAnalysisResponseDto createAnalysis(
            @PathVariable Long agencyId,
            @RequestBody SkillCoverageAnalysisRequestDto dto
    ) {
        SkillCoverageAnalysis analysis = skillCoverageAnalysisMapper.toEntity(dto);
        SkillCoverageAnalysis savedAnalysis = skillCoverageAnalysisService.createAnalysis(agencyId, analysis);
        return skillCoverageAnalysisMapper.toResponseDto(savedAnalysis);
    }

    // GET ALL ANALYSES OF AN AGENCY
    @GetMapping("/agency/{agencyId}")
    public List<SkillCoverageAnalysisResponseDto> getAnalysesByAgency(@PathVariable Long agencyId) {
        return skillCoverageAnalysisService.getAnalysesByAgency(agencyId)
                .stream()
                .map(skillCoverageAnalysisMapper::toResponseDto)
                .toList();
    }

    // GET LATEST ANALYSIS
    @GetMapping("/agency/{agencyId}/latest")
    public SkillCoverageAnalysisResponseDto getLatestAnalysis(@PathVariable Long agencyId) {
        SkillCoverageAnalysis analysis = skillCoverageAnalysisService.getLatestAnalysis(agencyId);
        return skillCoverageAnalysisMapper.toResponseDto(analysis);
    }
}