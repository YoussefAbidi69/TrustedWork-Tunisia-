package tn.esprit.userservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyPerformanceScoreResponseDto {

    private Long id;
    private Long agencyId;

    private Float deliveryRate;
    private Float clientSatisfaction;
    private Float responseTime;
    private Float memberRetention;

    private Float totalScore;
    private LocalDateTime computedAt;
}