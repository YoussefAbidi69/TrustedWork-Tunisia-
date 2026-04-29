package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.dto.LeaderboardDTO;
import com.trustedwork.module06.entity.Leaderboard;
import com.trustedwork.module06.repository.LeaderboardRepository;
import com.trustedwork.module06.repository.GrowthProfileRepository;
import com.trustedwork.module06.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;
    private final GrowthProfileRepository growthProfileRepository;
    private final org.springframework.web.client.RestTemplate loadBalancedRestTemplate;

    private static final String USER_SERVICE_URL = "http://user-service/api/identity/users/";

    @Override
    public List<LeaderboardDTO> getGlobalLeaderboard() {
        return leaderboardRepository.findAllByOrderByEngagementRankAsc()
                .stream().map(this::toDTO).toList();
    }

    @Override
    public List<LeaderboardDTO> getLeaderboardByGovernorate(String governorate) {
        return leaderboardRepository.findByGovernorateOrderByEngagementRankAsc(governorate)
                .stream().map(this::toDTO).toList();
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
                java.util.Map<String, Object> userData = loadBalancedRestTemplate.getForObject(USER_SERVICE_URL + gp.getUserId(), java.util.Map.class);
                if (userData != null && userData.get("location") != null) {
                    location = userData.get("location").toString().trim();
                    log.info(">>> [LEADERBOARD] Fetched real location for User {}: [{}]", gp.getUserId(), location);
                } else {
                    log.info(">>> [LEADERBOARD] No location found for User {}, using fallback: [{}]", gp.getUserId(), location);
                }
            } catch (Exception e) {
                log.error(">>> [LEADERBOARD] API Error fetching location for User {}: {}", gp.getUserId(), e.getMessage());
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
            log.info(">>> [LEADERBOARD] User {} rank {} in region {}", lb.getUserId(), lb.getEngagementRank(), lb.getGovernorate());
        }
        
        leaderboardRepository.saveAll(all);
        leaderboardRepository.flush();
    }

    private LeaderboardDTO toDTO(Leaderboard l) {
        String firstName = "Champion";
        String lastName  = "#" + l.getUserId();
        String photo     = null;

        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> userData = loadBalancedRestTemplate.getForObject(
                    USER_SERVICE_URL + l.getUserId(), java.util.Map.class);
            if (userData != null) {
                if (userData.get("firstName") != null) firstName = userData.get("firstName").toString();
                if (userData.get("lastName")  != null) lastName  = userData.get("lastName").toString();
                if (userData.get("photo")     != null) photo     = userData.get("photo").toString();
            }
        } catch (Exception e) {
            log.warn(">>> [LEADERBOARD] Could not fetch user info for userId={}: {}", l.getUserId(), e.getMessage());
        }

        return LeaderboardDTO.builder()
                .userId(l.getUserId())
                .governorate(l.getGovernorate())
                .engagementScore(l.getEngagementScore())
                .rank(l.getEngagementRank() != null ? l.getEngagementRank() : 0)
                .firstName(firstName)
                .lastName(lastName)
                .photo(photo)
                .build();
    }
}
