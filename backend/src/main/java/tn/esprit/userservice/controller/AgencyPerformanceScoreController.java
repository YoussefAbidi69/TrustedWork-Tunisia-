package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyPerformanceScoreRequestDto;
import tn.esprit.userservice.dto.AgencyPerformanceScoreResponseDto;
import tn.esprit.userservice.entity.AgencyPerformanceScore;
import tn.esprit.userservice.mapper.AgencyPerformanceScoreMapper;
import tn.esprit.userservice.service.IAgencyPerformanceScoreServices;

@RestController
@RequestMapping("/performance-scores")
@RequiredArgsConstructor
public class AgencyPerformanceScoreController {

    private final IAgencyPerformanceScoreServices performanceService;
    private final AgencyPerformanceScoreMapper agencyPerformanceScoreMapper;

    // CREATE OR UPDATE SCORE
    @PostMapping("/agency/{agencyId}")
    public AgencyPerformanceScoreResponseDto saveOrUpdateScore(
            @PathVariable Long agencyId,
            @RequestBody AgencyPerformanceScoreRequestDto dto
    ) {
        AgencyPerformanceScore score = agencyPerformanceScoreMapper.toEntity(dto);
        AgencyPerformanceScore savedScore = performanceService.saveOrUpdateScore(agencyId, score);
        return agencyPerformanceScoreMapper.toResponseDto(savedScore);
    }

    // GET SCORE BY AGENCY
    @GetMapping("/agency/{agencyId}")
    public AgencyPerformanceScoreResponseDto getScoreByAgency(@PathVariable Long agencyId) {
        AgencyPerformanceScore score = performanceService.getScoreByAgency(agencyId);
        return agencyPerformanceScoreMapper.toResponseDto(score);
    }
}