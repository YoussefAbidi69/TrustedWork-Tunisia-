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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
                .location(request.getLocation())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(request.getIsCurrent())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(workExperienceService.addWorkExperience(userId, exp)));
    }

    @PutMapping("/{expId}/user/{userId}")
    public ResponseEntity<WorkExperienceResponse> update(
            @PathVariable Long expId,
            @PathVariable Long userId,
            @Valid @RequestBody AddWorkExperienceRequest request) {

        WorkExperience exp = WorkExperience.builder()
                .jobTitle(request.getJobTitle())
                .company(request.getCompany())
                .location(request.getLocation())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(request.getIsCurrent())
                .build();

        return ResponseEntity.ok(
                toResponse(workExperienceService.updateWorkExperience(expId, userId, exp))
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkExperienceResponse>> getAll(@PathVariable Long userId) {
        List<WorkExperienceResponse> list = workExperienceService.getMyWorkExperiences(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/{expId}/user/{userId}")
    public ResponseEntity<WorkExperienceResponse> getById(
            @PathVariable Long expId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                toResponse(workExperienceService.getWorkExperienceById(expId, userId))
        );
    }

    @DeleteMapping("/{expId}/user/{userId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long expId,
            @PathVariable Long userId) {
        workExperienceService.deleteWorkExperience(expId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/total-duration")
    public ResponseEntity<Long> getTotalDuration(@PathVariable Long userId) {
        return ResponseEntity.ok(workExperienceService.getTotalExperienceInMonths(userId));
    }

    private WorkExperienceResponse toResponse(WorkExperience w) {
        long durationInMonths = calculateDurationInMonths(w.getStartDate(), w.getEndDate(), w.getIsCurrent());

        return WorkExperienceResponse.builder()
                .id(w.getId())
                .jobTitle(w.getJobTitle())
                .company(w.getCompany())
                .location(w.getLocation())
                .description(w.getDescription())
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .isCurrent(w.getIsCurrent())
                .periodLabel(buildPeriodLabel(w.getStartDate(), w.getEndDate(), w.getIsCurrent()))
                .durationLabel(buildDurationLabel(durationInMonths))
                .durationInMonths(durationInMonths)
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private long calculateDurationInMonths(LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        if (startDate == null) {
            return 0;
        }

        LocalDate effectiveEndDate = Boolean.TRUE.equals(isCurrent)
                ? LocalDate.now()
                : endDate;

        if (effectiveEndDate == null || startDate.isAfter(effectiveEndDate)) {
            return 0;
        }

        return java.time.temporal.ChronoUnit.MONTHS.between(startDate, effectiveEndDate);
    }

    private String buildPeriodLabel(LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        if (startDate == null) {
            return "";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRANCE);
        String start = startDate.format(formatter);

        if (Boolean.TRUE.equals(isCurrent)) {
            return start + " - Présent";
        }

        if (endDate == null) {
            return start;
        }

        return start + " - " + endDate.format(formatter);
    }

    private String buildDurationLabel(long durationInMonths) {
        if (durationInMonths <= 0) {
            return "Moins d'un mois";
        }

        long years = durationInMonths / 12;
        long months = durationInMonths % 12;

        if (years > 0 && months > 0) {
            return years + (years == 1 ? " an " : " ans ") + months + " mois";
        }

        if (years > 0) {
            return years + (years == 1 ? " an" : " ans");
        }

        return months + " mois";
    }
}