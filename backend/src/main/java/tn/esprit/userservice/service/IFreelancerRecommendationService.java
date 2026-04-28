package tn.esprit.userservice.service;

import org.springframework.data.domain.Pageable;
import tn.esprit.userservice.dto.FreelancerRecommendationDTO;
import tn.esprit.userservice.dto.RecommendationFilterDTO;
import tn.esprit.userservice.dto.RecommendationResponseDTO;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.FreelancerRecommendationScore;
import tn.esprit.userservice.entity.SkillCoverageAnalysis;
import tn.esprit.userservice.entity.User;

import java.util.List;

public interface IFreelancerRecommendationService {
    RecommendationResponseDTO getRecommendations(Long agencyId, Long requestingUserId, RecommendationFilterDTO filters, Pageable pageable);
    void recomputeScores(Long agencyId);
    FreelancerRecommendationScore computeScore(User freelancer, Agency agency, List<AgencyMember> currentMembers, SkillCoverageAnalysis latestCoverage);
}
