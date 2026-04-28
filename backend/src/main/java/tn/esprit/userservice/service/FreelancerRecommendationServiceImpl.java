package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import tn.esprit.userservice.dto.FreelancerRecommendationDTO;
import tn.esprit.userservice.dto.RecommendationFilterDTO;
import tn.esprit.userservice.dto.RecommendationResponseDTO;
import tn.esprit.userservice.dto.ScoreBreakdownDTO;
import tn.esprit.userservice.dto.FlaskRecommendationRequest;
import tn.esprit.userservice.dto.FlaskRecommendationResponse;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerRecommendationServiceImpl implements IFreelancerRecommendationService {

    private final FreelancerRecommendationScoreRepository scoreRepository;
    private final IAgencyRepository agencyRepository;
    private final IAgencyMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final IAgencyInvitationRepository invitationRepository;
    private final RestTemplate mlRestTemplate;

    @Override
    @Transactional
    public RecommendationResponseDTO getRecommendations(Long agencyId, Long requestingUserId, RecommendationFilterDTO filters, Pageable pageable) {
        verifyLead(agencyId, requestingUserId);

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        Optional<FreelancerRecommendationScore> latest = scoreRepository.findTopByAgencyIdOrderByComputedAtDesc(agencyId);
        
        boolean refresh = filters != null && filters.isRefresh();
        boolean needsComputation = refresh || latest.isEmpty() || latest.get().getComputedAt() == null || latest.get().getComputedAt().isBefore(LocalDateTime.now().minusHours(1));
        
        if (needsComputation) {
            log.info("Triggering async score computation for agency {}", agencyId);
            if(latest.isEmpty() || refresh) {
                recomputeScoresSync(agencyId);
            } else {
                triggerAsyncRecomputation(agencyId);
            }
        }

        Page<FreelancerRecommendationScore> scoresPage = scoreRepository.findByAgencyIdOrderByRecommendationScoreDesc(agencyId, Pageable.unpaged());
        List<FreelancerRecommendationScore> allScores = scoresPage.getContent();

        if (filters != null) {
            allScores = allScores.stream().filter(score -> {
                User f = score.getFreelancer();
                if (filters.getMinScore() != null && score.getRecommendationScore() < filters.getMinScore()) return false;
                if (filters.getAvailability() != null && !filters.getAvailability().equalsIgnoreCase(f.getAvailability())) return false;
                if (filters.getSkills() != null && !filters.getSkills().isEmpty()) {
                    List<String> requiredSkills = parseSkills(filters.getSkills());
                    List<String> fSkills = parseSkills(f.getSkills());
                    if (!fSkills.containsAll(requiredSkills)) return false;
                }
                if (filters.getSearch() != null && !filters.getSearch().trim().isEmpty()) {
                    String search = filters.getSearch().toLowerCase();
                    boolean matchName = (f.getFirstName() + " " + f.getLastName()).toLowerCase().contains(search);
                    boolean matchSkills = f.getSkills() != null && f.getSkills().toLowerCase().contains(search);
                    if (!matchName && !matchSkills) return false;
                }
                return true;
            }).collect(Collectors.toList());
        }

        if (filters != null && filters.getSortBy() != null) {
            if (filters.getSortBy().equalsIgnoreCase("trust")) {
                allScores.sort((a, b) -> {
                    int cmp = Float.compare(b.getTrustScore(), a.getTrustScore());
                    return cmp != 0 ? cmp : Float.compare(b.getRecommendationScore(), a.getRecommendationScore());
                });
            } else if (filters.getSortBy().equalsIgnoreCase("availability")) {
                allScores.sort((a, b) -> {
                    int cmp = Float.compare(b.getAvailabilityScore(), a.getAvailabilityScore());
                    return cmp != 0 ? cmp : Float.compare(b.getRecommendationScore(), a.getRecommendationScore());
                });
            } else {
                allScores.sort((a, b) -> Float.compare(b.getRecommendationScore(), a.getRecommendationScore()));
            }
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allScores.size());
        List<FreelancerRecommendationScore> paginatedScores = start <= end ? allScores.subList(start, end) : new ArrayList<>();

        List<FreelancerRecommendationDTO> dtos = paginatedScores.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return RecommendationResponseDTO.builder()
                .agencyId(agencyId)
                .agencyName(agency.getName())
                .totalCandidates(allScores.size())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .recommendations(dtos)
                .build();
    }

    private void verifyLead(Long agencyId, Long userId) {
        AgencyMember member = memberRepository.findByAgencyIdAndUserId(agencyId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this agency"));
        if (member.getRole() != MemberRole.LEAD) {
            throw new RuntimeException("Only a LEAD can access recommendations");
        }
    }

    @Async
    public void triggerAsyncRecomputation(Long agencyId) {
        recomputeScoresSync(agencyId);
    }

    @Override
    @Transactional
    public void recomputeScores(Long agencyId) {
        recomputeScoresSync(agencyId);
    }

    @Transactional
    protected void recomputeScoresSync(Long agencyId) {
        log.info("Starting recommendation scoring for agency {}", agencyId);
        Agency agency = agencyRepository.findById(agencyId).orElseThrow();
        List<AgencyMember> currentMembers = memberRepository.findByAgencyIdAndStatus(agencyId, MemberStatus.ACTIVE);
        
        SkillCoverageAnalysis latestCoverage = null;
        if (agency.getSkillCoverageAnalyses() != null && !agency.getSkillCoverageAnalyses().isEmpty()) {
            latestCoverage = agency.getSkillCoverageAnalyses().stream()
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        List<User> eligibleFreelancers = userRepository.findEligibleFreelancers(agencyId);

        if (eligibleFreelancers.isEmpty()) {
            log.info("No eligible freelancers found for agency {}", agencyId);
            return;
        }

        // 1. Prepare candidates
        List<FlaskRecommendationRequest.FlaskCandidateDTO> candidates = new ArrayList<>();
        Map<Long, User> userMap = new HashMap<>();
        
        for (User freelancer : eligibleFreelancers) {
            float f1 = computeSkillMatch(freelancer, currentMembers, latestCoverage);
            float f2 = computeTrustScore(freelancer);
            float f3 = computeExperienceScore(freelancer);
            float f4 = computeAvailabilityScore(freelancer);
            float f5 = computeSimilarityScore(freelancer, currentMembers);
            float f6 = computeLocationScore(freelancer, agency);
            float kyc = (freelancer.getKycStatus() == KycStatus.APPROVED) ? 1.0f : (freelancer.getKycStatus() == KycStatus.IN_REVIEW ? 0.5f : 0.0f);
            float liveness = freelancer.isLivenessPassed() ? 1.0f : 0.0f;

            candidates.add(FlaskRecommendationRequest.FlaskCandidateDTO.builder()
                    .freelancer_id(freelancer.getId())
                    .skill_match_score(f1)
                    .trust_score(f2)
                    .experience_score(f3)
                    .availability_score(f4)
                    .similarity_score(f5)
                    .location_score(f6)
                    .kyc_bonus(kyc)
                    .liveness_bonus(liveness)
                    .build());
                    
            userMap.put(freelancer.getId(), freelancer);
        }

        FlaskRecommendationRequest request = FlaskRecommendationRequest.builder()
                .agency_id(agencyId)
                .candidates(candidates)
                .build();

        List<FreelancerRecommendationScore> newScores = new ArrayList<>();
        
        try {
            log.info("Calling Flask ML API for {} candidates", candidates.size());
            ResponseEntity<FlaskRecommendationResponse> response = mlRestTemplate.postForEntity(
                    "http://localhost:5001/recommend", request, FlaskRecommendationResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                for (FlaskRecommendationResponse.FlaskRecommendationDTO rec : response.getBody().getRecommendations()) {
                    User freelancer = userMap.get(rec.getFreelancer_id());
                    newScores.add(FreelancerRecommendationScore.builder()
                            .agency(agency)
                            .freelancer(freelancer)
                            .recommendationScore(rec.getRecommendation_score())
                            .skillMatchScore(rec.getSkill_match_score())
                            .trustScore(rec.getTrust_score())
                            .availabilityScore(rec.getAvailability_score())
                            .experienceScore(rec.getExperience_score())
                            .similarityScore(rec.getSimilarity_score())
                            .locationScore(rec.getLocation_score())
                            .explanation(rec.getExplanation())
                            .computedAt(LocalDateTime.now())
                            .build());
                }
            } else {
                throw new RuntimeException("Flask API returned error or empty");
            }
        } catch (Exception e) {
            log.error("Failed to fetch from Flask ML API. Using fallback rule-based scoring. Error: {}", e.getMessage());
            for (FlaskRecommendationRequest.FlaskCandidateDTO cand : candidates) {
                User freelancer = userMap.get(cand.getFreelancer_id());
                newScores.add(computeScoreFallback(freelancer, agency, cand));
            }
        }

        scoreRepository.deleteByAgencyId(agencyId);
        scoreRepository.flush();
        scoreRepository.saveAll(newScores);
        log.info("Saved {} recommendation scores for agency {}", newScores.size(), agencyId);
    }

    private FreelancerRecommendationScore computeScoreFallback(User freelancer, Agency agency, FlaskRecommendationRequest.FlaskCandidateDTO cand) {
        float f1 = cand.getSkill_match_score();
        float f2 = cand.getTrust_score();
        float f3 = cand.getAvailability_score();
        float f4 = cand.getExperience_score();
        float f5 = cand.getSimilarity_score();
        float f6 = cand.getLocation_score();

        float totalScore = (f1 * 0.40f) + (f2 * 0.30f) + (f3 * 0.15f) + (f4 * 0.10f) + (f6 * 0.05f);
        totalScore = Math.round(totalScore * 10000f) / 10000f;

        String expTrust = f2 >= 0.8 ? "High" : (f2 >= 0.4 ? "Medium" : "Low");
        String expAvail = "UNKNOWN";
        if(f3 >= 1.0) expAvail = "FULL_TIME";
        else if (f3 >= 0.6) expAvail = "PART_TIME";
        else if (f3 >= 0.3) expAvail = "WEEKENDS";
        else if (f3 == 0.0) expAvail = "UNAVAILABLE";

        int expYears = Math.round(f4 * 10);
        String loc = f6 == 1.0f ? "match" : (f6 >= 0.4f ? "nearby" : "remote");

        String explanation = String.format("Skill match: %d%% | Trust: %s | Availability: %s | Experience: %d years | Similar to team: %d%% | Location: (%s)",
                (int)(f1 * 100), expTrust, expAvail, expYears, (int)(f5 * 100), loc);

        return FreelancerRecommendationScore.builder()
                .agency(agency)
                .freelancer(freelancer)
                .recommendationScore(totalScore)
                .skillMatchScore(f1)
                .trustScore(f2)
                .availabilityScore(f3)
                .experienceScore(f4)
                .similarityScore(f5)
                .locationScore(f6)
                .explanation(explanation)
                .computedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public FreelancerRecommendationScore computeScore(User freelancer, Agency agency, List<AgencyMember> currentMembers, SkillCoverageAnalysis latestCoverage) {
        // Obsolete: Handled in recomputeScoresSync via fallback or Flask
        return null;
    }

    private float computeSkillMatch(User freelancer, List<AgencyMember> members, SkillCoverageAnalysis coverage) {
        List<String> fSkills = parseSkills(freelancer.getSkills());
        if (fSkills.isEmpty()) return 0f;

        Set<String> targetSkills = new HashSet<>();
        if (coverage != null && coverage.getMissingSkills() != null && !coverage.getMissingSkills().isEmpty()) {
            targetSkills.addAll(parseSkills(coverage.getMissingSkills()));
        } else {
            for (AgencyMember m : members) {
                targetSkills.addAll(parseSkills(m.getSkills()));
            }
        }

        if (targetSkills.isEmpty()) return 0f;

        long intersection = fSkills.stream().filter(targetSkills::contains).count();
        return (float) intersection / targetSkills.size();
    }

    private float computeTrustScore(User freelancer) {
        return (Math.max(freelancer.getTrustLevel(), 1) - 1) / 4.0f;
    }

    private float computeAvailabilityScore(User freelancer) {
        if (freelancer.getAvailability() == null) return 0.0f;
        String av = freelancer.getAvailability().toUpperCase();
        if (av.contains("FULL")) return 1.0f;
        if (av.contains("PART")) return 0.6f;
        if (av.contains("WEEKEND")) return 0.3f;
        if (av.contains("UNAVAIL")) return 0.0f;
        return 0.0f;
    }

    private float computeExperienceScore(User freelancer) {
        if (freelancer.getExperience() != null) {
            Pattern pattern = Pattern.compile("(\\d+)\\s*year");
            Matcher m = pattern.matcher(freelancer.getExperience());
            if (m.find()) {
                int years = Integer.parseInt(m.group(1));
                return Math.min(years, 10) / 10.0f;
            }
        }
        return 0.0f;
    }

    private float computeSimilarityScore(User freelancer, List<AgencyMember> members) {
        if (members.isEmpty()) return 0f;
        List<String> fSkills = parseSkills(freelancer.getSkills());
        if (fSkills.isEmpty()) return 0f;

        float totalSimilarity = 0f;
        for (AgencyMember m : members) {
            List<String> mSkills = parseSkills(m.getSkills());
            if (mSkills.isEmpty()) continue;
            Set<String> union = new HashSet<>(fSkills);
            union.addAll(mSkills);
            long intersection = fSkills.stream().filter(mSkills::contains).count();
            float sim = (float) intersection / union.size();
            totalSimilarity += sim;
        }
        return totalSimilarity / members.size();
    }

    private float computeLocationScore(User freelancer, Agency agency) {
        String fLoc = freelancer.getLocation();
        String aLoc = agency.getCity();
        if (fLoc == null) return 0.2f;
        
        List<String> tunisianCities = Arrays.asList("tunis", "sfax", "sousse", "monastir", "ariana", "ben arous", "manouba", "bizerte", "nabeul", "mahdia");
        boolean isFTunisian = tunisianCities.stream().anyMatch(c -> fLoc.toLowerCase().contains(c));

        if (aLoc != null && fLoc.equalsIgnoreCase(aLoc)) return 1.0f;
        if (isFTunisian) return 0.5f;
        return 0.2f;
    }

    private List<String> parseSkills(String skills) {
        if (skills == null || skills.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(skills.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private FreelancerRecommendationDTO mapToDTO(FreelancerRecommendationScore score) {
        User f = score.getFreelancer();
        return FreelancerRecommendationDTO.builder()
                .freelancerId(f.getId())
                .firstName(f.getFirstName())
                .lastName(f.getLastName())
                .email(f.getEmail())
                .headline(f.getHeadline())
                .bio(f.getBio())
                .location(f.getLocation())
                .skills(parseSkills(f.getSkills()))
                .availability(f.getAvailability())
                .trustLevel(f.getTrustLevel())
                .kycStatus(f.getKycStatus() != null ? f.getKycStatus().name() : null)
                .photo(f.getPhoto())
                .recommendationScore(score.getRecommendationScore())
                .scoreBreakdown(ScoreBreakdownDTO.builder()
                        .skillMatch(score.getSkillMatchScore())
                        .trust(score.getTrustScore())
                        .availability(score.getAvailabilityScore())
                        .experience(score.getExperienceScore())
                        .similarity(score.getSimilarityScore())
                        .location(score.getLocationScore())
                        .build())
                .explanation(score.getExplanation())
                .alreadyInvited(false)
                .invitationStatus(null)
                .build();
    }
}
