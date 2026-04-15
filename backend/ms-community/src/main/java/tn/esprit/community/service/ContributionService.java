package tn.esprit.community.service;

import tn.esprit.community.entity.Contribution;

public interface ContributionService {
    Contribution recordSharedCourse(Long userId);
    Contribution getContribution(Long userId);
}
