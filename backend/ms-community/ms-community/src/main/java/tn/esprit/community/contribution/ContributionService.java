package tn.esprit.community.contribution;

public interface ContributionService {
    Contribution recordSharedCourse(Long userId);
    Contribution getContribution(Long userId);
}
