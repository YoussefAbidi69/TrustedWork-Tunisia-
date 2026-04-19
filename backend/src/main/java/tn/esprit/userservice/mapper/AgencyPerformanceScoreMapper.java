package tn.esprit.userservice.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.userservice.dto.AgencyPerformanceScoreRequestDto;
import tn.esprit.userservice.dto.AgencyPerformanceScoreResponseDto;
import tn.esprit.userservice.entity.AgencyPerformanceScore;

@Component
public class AgencyPerformanceScoreMapper {

    public AgencyPerformanceScore toEntity(AgencyPerformanceScoreRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return AgencyPerformanceScore.builder()
                .deliveryRate(dto.getDeliveryRate())
                .clientSatisfaction(dto.getClientSatisfaction())
                .responseTime(dto.getResponseTime())
                .memberRetention(dto.getMemberRetention())
                .build();
    }

    public AgencyPerformanceScoreResponseDto toResponseDto(AgencyPerformanceScore score) {
        if (score == null) {
            return null;
        }

        return AgencyPerformanceScoreResponseDto.builder()
                .id(score.getId())
                .agencyId(score.getAgency().getId())
                .deliveryRate(score.getDeliveryRate())
                .clientSatisfaction(score.getClientSatisfaction())
                .responseTime(score.getResponseTime())
                .memberRetention(score.getMemberRetention())
                .totalScore(score.getTotalScore())
                .computedAt(score.getComputedAt())
                .build();
    }
}