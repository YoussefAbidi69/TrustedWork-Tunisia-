package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.Agency;
import tn.esprit.userservice.entity.AgencyPerformanceScore;
import tn.esprit.userservice.repository.IAgencyPerformanceScoreRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.service.IAgencyPerformanceScoreServices;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgencyPerformanceScoreServiceImpl implements IAgencyPerformanceScoreServices {

    private final IAgencyPerformanceScoreRepository agencyPerformanceScoreRepository;
    private final IAgencyRepository agencyRepository;

    @Override
    public AgencyPerformanceScore saveOrUpdateScore(Long agencyId, AgencyPerformanceScore score) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        AgencyPerformanceScore existingScore = agencyPerformanceScoreRepository.findByAgencyId(agencyId)
                .orElse(null);

        if (existingScore != null) {
            existingScore.setDeliveryRate(score.getDeliveryRate());
            existingScore.setClientSatisfaction(score.getClientSatisfaction());
            existingScore.setResponseTime(score.getResponseTime());
            existingScore.setMemberRetention(score.getMemberRetention());
            existingScore.setTotalScore(calculateTotalScore(
                    score.getDeliveryRate(),
                    score.getClientSatisfaction(),
                    score.getResponseTime(),
                    score.getMemberRetention()
            ));
            existingScore.setComputedAt(LocalDateTime.now());

            return agencyPerformanceScoreRepository.save(existingScore);
        }

        score.setAgency(agency);
        score.setTotalScore(calculateTotalScore(
                score.getDeliveryRate(),
                score.getClientSatisfaction(),
                score.getResponseTime(),
                score.getMemberRetention()
        ));
        score.setComputedAt(LocalDateTime.now());

        return agencyPerformanceScoreRepository.save(score);
    }

    @Override
    public AgencyPerformanceScore getScoreByAgency(Long agencyId) {
        return agencyPerformanceScoreRepository.findByAgencyId(agencyId)
                .orElseThrow(() -> new RuntimeException("Performance score not found for this agency"));
    }

    @Override
    public Float calculateTotalScore(Float deliveryRate, Float clientSatisfaction, Float responseTime, Float memberRetention) {
        float dr = deliveryRate != null ? deliveryRate : 0f;
        float cs = clientSatisfaction != null ? clientSatisfaction : 0f;
        float rt = responseTime != null ? responseTime : 0f;
        float mr = memberRetention != null ? memberRetention : 0f;

        return (dr * 0.35f) + (cs * 0.30f) + (rt * 0.20f) + (mr * 0.15f);
    }
}