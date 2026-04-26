package com.trustedwork.module06.dto;

import com.trustedwork.module06.enums.ChallengeStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDTO {
    private Long id;
    private String title;
    private String description;
    private int xpReward;
    private String deadline;
    private String challengeTypeCode;
    private ChallengeStatus status;
    private ChallengeParticipationDTO currentParticipation;
}
