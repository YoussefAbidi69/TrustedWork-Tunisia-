package tn.esprit.userservice.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyPerformanceScoreRequestDto {

    private Float deliveryRate;
    private Float clientSatisfaction;
    private Float responseTime;
    private Float memberRetention;
}
