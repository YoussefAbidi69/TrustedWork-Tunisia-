package com.trustedwork.module06.dto;

import com.trustedwork.module06.enums.ParticipationStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipationDTO {
    private Long id;
    private Long challengeId;
    private Long userId;
    private ParticipationStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;
}
