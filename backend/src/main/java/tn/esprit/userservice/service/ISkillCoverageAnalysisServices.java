package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.SkillCoverageAnalysis;

import java.util.List;

public interface ISkillCoverageAnalysisServices {

    SkillCoverageAnalysis createAnalysis(Long agencyId, SkillCoverageAnalysis analysis);

    List<SkillCoverageAnalysis> getAnalysesByAgency(Long agencyId);

    SkillCoverageAnalysis getLatestAnalysis(Long agencyId);

    Float calculateCoveragePercentage(String coveredSkills, String missingSkills);
}