package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.LeaderboardDTO;
import com.trustedwork.module06.entity.Leaderboard;
import com.trustedwork.module06.repository.LeaderboardRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final GrowthProfileRepository growthProfileRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://localhost:8081/api/identity/users/";

    @Override
    public List<LeaderboardDTO> getGlobalLeaderboard() {
        return leaderboardRepository.findAllByOrderByEngagementRankAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardDTO> getLeaderboardByGovernorate(String governorate) {
        return leaderboardRepository.findByGovernorateOrderByEngagementRankAsc(governorate)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    @SuppressWarnings("unchecked")
    public void recomputeAllRanks() {
        // 1. Fetch all GrowthProfiles
        List<com.trustedwork.module06.entity.GrowthProfile> profiles = 
            growthProfileRepository.findAll();
        
        // 2. Sync with Leaderboard table and ensure scores are updated
        for (com.trustedwork.module06.entity.GrowthProfile gp : profiles) {
            String location = "Tunis"; // Fallback
            try {
                // Fetch real location from Identity service
                java.util.Map<String, Object> userData = restTemplate.getForObject(USER_SERVICE_URL + gp.getUserId(), java.util.Map.class);
                if (userData != null && userData.get("location") != null) {
                    location = userData.get("location").toString().trim();
                    System.out.println(">>> [LEADERBOARD] Fetched real location for User " + gp.getUserId() + ": [" + location + "]");
                } else {
                    System.out.println(">>> [LEADERBOARD] No location found for User " + gp.getUserId() + ", using fallback: [" + location + "]");
                }
            } catch (Exception e) {
                System.err.println(">>> [LEADERBOARD] API Error fetching location for User " + gp.getUserId() + ": " + e.getMessage());
            }

            Leaderboard lb = leaderboardRepository.findByUserId(gp.getUserId())
                .orElse(Leaderboard.builder()
                            .userId(gp.getUserId())
                            .build());
            
            lb.setGovernorate(location);
            
            // Nouvelle Logique
            double calculatedScore = gp.getXpPoints() + (gp.getLevel() * 250.0);
            lb.setEngagementScore(calculatedScore);
            
            leaderboardRepository.save(lb);
        }

        // 3. Sort and assign numeric ranks
        List<Leaderboard> all = leaderboardRepository.findAll();
        all.sort((a, b) -> Double.compare(b.getEngagementScore(), a.getEngagementScore()));
        
        for (int i = 0; i < all.size(); i++) {
            Leaderboard lb = all.get(i);
            lb.setEngagementRank(i + 1);
            System.out.println(">>> [LEADERBOARD] User " + lb.getUserId() + " rank " + lb.getEngagementRank() + " in region " + lb.getGovernorate());
        }
        
        leaderboardRepository.saveAll(all);
        leaderboardRepository.flush();
    }

    private LeaderboardDTO toDTO(Leaderboard l) {
        return LeaderboardDTO.builder()
                .userId(l.getUserId())
                .governorate(l.getGovernorate())
                .engagementScore(l.getEngagementScore())
                .rank(l.getEngagementRank() != null ? l.getEngagementRank() : 0)
                .firstName("Champion")
                .lastName("#" + l.getUserId())
                .photo(null)
                .build();
    }
}
