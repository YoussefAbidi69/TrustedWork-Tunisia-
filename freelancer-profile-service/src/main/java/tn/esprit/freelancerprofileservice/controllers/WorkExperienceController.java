package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddWorkExperienceRequest;
import tn.esprit.freelancerprofileservice.dto.response.WorkExperienceResponse;
import tn.esprit.freelancerprofileservice.entities.WorkExperience;
import tn.esprit.freelancerprofileservice.services.IWorkExperienceService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion des expériences professionnelles
 */
@RestController
@RequestMapping("/api/work-experiences")
@RequiredArgsConstructor
public class WorkExperienceController {

    private final IWorkExperienceService workExperienceService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<WorkExperienceResponse> add(
            @PathVariable Long userId,
            @Valid @RequestBody AddWorkExperienceRequest request) {

        WorkExperience exp = WorkExperience.builder()
                .jobTitle(request.getJobTitle())
                .company(request.getCompany())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(request.getIsCurrent())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(workExperienceService.addWorkExperience(userId, exp)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkExperienceResponse>> getAll(@PathVariable Long userId) {
        List<WorkExperienceResponse> list = workExperienceService.getMyWorkExperiences(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{expId}/user/{userId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long expId,
            @PathVariable Long userId) {
        workExperienceService.deleteWorkExperience(expId, userId);
        return ResponseEntity.noContent().build();
    }

    private WorkExperienceResponse toResponse(WorkExperience w) {
        return WorkExperienceResponse.builder()
                .id(w.getId())
                .jobTitle(w.getJobTitle())
                .company(w.getCompany())
                .description(w.getDescription())
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .isCurrent(w.getIsCurrent())
                .build();
    }
}