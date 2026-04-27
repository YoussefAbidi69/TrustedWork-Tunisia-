package com.trustedwork.module06.service;

import com.trustedwork.module06.dto.LeaderboardDTO;
import java.util.List;

public interface LeaderboardService {
    List<LeaderboardDTO> getGlobalLeaderboard();
    List<LeaderboardDTO> getLeaderboardByGovernorate(String governorate);
    void recomputeAllRanks();
}
