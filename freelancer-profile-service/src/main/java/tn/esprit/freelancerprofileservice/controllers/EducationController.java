package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddEducationRequest;
import tn.esprit.freelancerprofileservice.dto.response.EducationResponse;
import tn.esprit.freelancerprofileservice.entities.Education;
import tn.esprit.freelancerprofileservice.services.IEducationService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion du parcours académique
 */
@RestController
@RequestMapping("/api/educations")
@RequiredArgsConstructor
public class EducationController {

    private final IEducationService educationService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<EducationResponse> add(
            @PathVariable Long userId,
            @Valid @RequestBody AddEducationRequest request) {

        Education edu = Education.builder()
                .degree(request.getDegree())
                .institution(request.getInstitution())
                .fieldOfStudy(request.getFieldOfStudy())
                .graduationYear(request.getGraduationYear())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(educationService.addEducation(userId, edu)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EducationResponse>> getAll(@PathVariable Long userId) {
        List<EducationResponse> list = educationService.getMyEducations(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{eduId}/user/{userId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long eduId,
            @PathVariable Long userId) {
        educationService.deleteEducation(eduId, userId);
        return ResponseEntity.noContent().build();
    }

    private EducationResponse toResponse(Education e) {
        return EducationResponse.builder()
                .id(e.getId())
                .degree(e.getDegree())
                .institution(e.getInstitution())
                .fieldOfStudy(e.getFieldOfStudy())
                .graduationYear(e.getGraduationYear())
                .build();
    }
}