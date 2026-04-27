package com.trustedwork.module06.mapper;

import com.trustedwork.module06.dto.ChallengeDTO;
import com.trustedwork.module06.dto.ChallengeParticipationDTO;
import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.entity.ChallengeParticipation;

public class ChallengeMapper {

    private ChallengeMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static ChallengeDTO toDTO(Challenge challenge) {
        if (challenge == null) return null;
        return ChallengeDTO.builder()
                .id(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .xpReward(challenge.getXpReward())
                .deadline(challenge.getDeadline() != null ? challenge.getDeadline().toString() : null)
                .challengeTypeCode(challenge.getChallengeTypeCode())
                .status(challenge.getStatus())
                .build();
    }

    public static Challenge toEntity(ChallengeDTO dto) {
        if (dto == null) return null;
        Challenge.ChallengeBuilder builder = Challenge.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .xpReward(dto.getXpReward())
                .challengeTypeCode(dto.getChallengeTypeCode())
                .status(dto.getStatus());

        if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
            try {
                if (dto.getDeadline().length() == 10) {
                    builder.deadline(java.time.LocalDate.parse(dto.getDeadline()).atStartOfDay());
                } else {
                    builder.deadline(java.time.LocalDateTime.parse(dto.getDeadline()));
                }
            } catch (Exception e) {}
        }
        return builder.build();
    }

    public static ChallengeParticipationDTO toParticipationDTO(ChallengeParticipation p) {
        if (p == null) return null;
        return ChallengeParticipationDTO.builder()
                .id(p.getId())
                .challengeId(p.getChallenge().getId())
                .userId(p.getUserId())
                .status(p.getStatus())
                .joinedAt(p.getJoinedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}
