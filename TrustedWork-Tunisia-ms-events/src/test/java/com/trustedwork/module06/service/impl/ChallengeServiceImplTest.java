package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.entity.ChallengeParticipation;
import com.trustedwork.module06.enums.ParticipationStatus;
import com.trustedwork.module06.repository.ChallengeParticipationRepository;
import com.trustedwork.module06.repository.ChallengeRepository;
import com.trustedwork.module06.repository.EventRegistrationRepository;
import com.trustedwork.module06.repository.UserBadgeRepository;
import com.trustedwork.module06.service.GamificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private ChallengeParticipationRepository participationRepository;
    @Mock
    private EventRegistrationRepository eventRegistrationRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;
    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Test
    void testJoinChallenge_Success() {
        Long userId = 1L;
        Long challengeId = 10L;
        Challenge challenge = Challenge.builder().id(challengeId).build();

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(participationRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.empty());

        challengeService.joinChallenge(userId, challengeId);

        verify(participationRepository).save(any(ChallengeParticipation.class));
    }

    @Test
    void testSucceedChallenge_Manual() {
        Long userId = 1L;
        Long challengeId = 10L;
        ChallengeParticipation p = ChallengeParticipation.builder()
                .userId(userId)
                .challenge(Challenge.builder().challengeTypeCode("MANUAL").build())
                .status(ParticipationStatus.JOINED)
                .build();

        when(participationRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(p));

        challengeService.succeedChallenge(userId, challengeId);

        assertEquals(ParticipationStatus.SUCCESS, p.getStatus());
        verify(participationRepository).save(p);
    }
}
