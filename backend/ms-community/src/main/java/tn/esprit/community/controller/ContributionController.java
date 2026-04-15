package tn.esprit.community.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.community.entity.Contribution;
import tn.esprit.community.service.ContributionService;

@RestController
@RequestMapping("/api/contributions")
public class ContributionController {
    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @PostMapping("/users/{userId}/record")
    public ResponseEntity<Contribution> recordSharedCourse(@PathVariable Long userId) {
        Contribution contribution = contributionService.recordSharedCourse(userId);
        return new ResponseEntity<>(contribution, HttpStatus.OK);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Contribution> getContribution(@PathVariable Long userId) {
        Contribution contribution = contributionService.getContribution(userId);
        if (contribution == null) {
            contribution = Contribution.builder()
                    .id(0L)
                    .userId(userId)
                    .sharedCourseCount(0)
                    .build();
        }
        return new ResponseEntity<>(contribution, HttpStatus.OK);
    }
}
