package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.AgencyPerformanceScore;

public interface IAgencyPerformanceScoreServices {

    AgencyPerformanceScore saveOrUpdateScore(Long agencyId, AgencyPerformanceScore score);

    AgencyPerformanceScore getScoreByAgency(Long agencyId);

    Float calculateTotalScore(Float deliveryRate, Float clientSatisfaction, Float responseTime, Float memberRetention);
}