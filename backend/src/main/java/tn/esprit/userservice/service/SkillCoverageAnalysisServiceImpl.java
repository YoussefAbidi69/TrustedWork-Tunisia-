package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.entity.SkillCoverageAnalysis;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.ISkillCoverageAnalysisRepository;
import tn.esprit.userservice.service.ISkillCoverageAnalysisServices;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillCoverageAnalysisServiceImpl implements ISkillCoverageAnalysisServices {

    private final ISkillCoverageAnalysisRepository skillCoverageAnalysisRepository;
    private final IAgencyRepository agencyRepository;

    @Override
    public SkillCoverageAnalysis createAnalysis(Long agencyId, SkillCoverageAnalysis analysis) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        analysis.setAgency(agency);
        analysis.setCoveragePercentage(
                calculateCoveragePercentage(
                        analysis.getCoveredSkills(),
                        analysis.getMissingSkills()
                )
        );
        analysis.setAnalyzedAt(LocalDateTime.now());

        return skillCoverageAnalysisRepository.save(analysis);
    }

    @Override
    public List<SkillCoverageAnalysis> getAnalysesByAgency(Long agencyId) {
        return skillCoverageAnalysisRepository.findByAgencyIdOrderByAnalyzedAtDesc(agencyId);
    }

    @Override
    public SkillCoverageAnalysis getLatestAnalysis(Long agencyId) {
        return skillCoverageAnalysisRepository.findFirstByAgencyIdOrderByAnalyzedAtDesc(agencyId)
                .orElseThrow(() -> new RuntimeException("No skill coverage analysis found for this agency"));
    }

    @Override
    public Float calculateCoveragePercentage(String coveredSkills, String missingSkills) {
        int coveredCount = countSkills(coveredSkills);
        int missingCount = countSkills(missingSkills);
        int total = coveredCount + missingCount;

        if (total == 0) {
            return 0f;
        }

        return (coveredCount * 100f) / total;
    }

    private int countSkills(String skills) {
        if (skills == null || skills.trim().isEmpty()) {
            return 0;
        }

        return (int) java.util.Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
    }
}