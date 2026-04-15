package tn.esprit.community.service.impl;

import org.springframework.stereotype.Service;
import tn.esprit.community.entity.Contribution;
import tn.esprit.community.repository.ContributionRepository;
import tn.esprit.community.service.ContributionService;

@Service
public class ContributionServiceImpl implements ContributionService {
    private final ContributionRepository contributionRepository;

    public ContributionServiceImpl(ContributionRepository contributionRepository) {
        this.contributionRepository = contributionRepository;
    }

    @Override
    public Contribution recordSharedCourse(Long userId) {
        Contribution contribution = contributionRepository.findByUserId(userId)
                .orElse(Contribution.builder().userId(userId).sharedCourseCount(0).build());
        contribution.setSharedCourseCount(contribution.getSharedCourseCount() + 1);
        return contributionRepository.save(contribution);
    }

    @Override
    public Contribution getContribution(Long userId) {
        return contributionRepository.findByUserId(userId).orElse(null);
    }
}
