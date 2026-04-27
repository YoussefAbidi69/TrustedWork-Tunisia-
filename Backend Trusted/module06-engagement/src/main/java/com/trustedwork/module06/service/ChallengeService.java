package com.trustedwork.module06.service;

import com.trustedwork.module06.dto.ChallengeDTO;
import java.util.List;

public interface ChallengeService {
    // Admin
    List<ChallengeDTO> getAllChallenges();
    ChallengeDTO createChallenge(ChallengeDTO challengeDTO);
    ChallengeDTO updateChallenge(Long id, ChallengeDTO challengeDTO);
    void deleteChallenge(Long id);
    
    // User
    List<ChallengeDTO> getActiveChallenges(Long userId); // Updated to show participation
    void joinChallenge(Long userId, Long challengeId);
    void succeedChallenge(Long userId, Long challengeId); // For simulation/verification
    void claimReward(Long userId, Long challengeId);
}
