package tn.esprit.freelancerprofileservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddEducationRequest;
import tn.esprit.freelancerprofileservice.dto.request.UpdateEducationRequest;
import tn.esprit.freelancerprofileservice.dto.response.EducationResponse;
import tn.esprit.freelancerprofileservice.entities.Education;
import tn.esprit.freelancerprofileservice.services.IEducationService;

import java.util.List;

/**
 * Controller REST — gestion du parcours académique
 *
 * Endpoints :
 * POST   /api/educations/user/{userId}               → ajouter une formation
 * GET    /api/educations/user/{userId}               → lister (triées par année DESC)
 * PUT    /api/educations/{eduId}/user/{userId}       → modifier une formation
 * DELETE /api/educations/{eduId}/user/{userId}       → supprimer une formation
 */
@RestController
@RequestMapping("/api/educations")
@RequiredArgsConstructor
@Tag(name = "Education", description = "Gestion du parcours académique du freelancer")
public class EducationController {

    private final IEducationService educationService;

    // ─── Ajouter une formation ───────────────────────────────────────────────
    @PostMapping("/user/{userId}")
    @Operation(summary = "Ajouter une formation académique")
    public ResponseEntity<Object> add(
            @PathVariable Long userId,
            @Valid @RequestBody AddEducationRequest request) {

        try {
            Education edu = Education.builder()
                    .degree(request.getDegree().trim())
                    .institution(request.getInstitution().trim())
                    .fieldOfStudy(request.getFieldOfStudy() != null
                            ? request.getFieldOfStudy().trim() : null)
                    .graduationYear(request.getGraduationYear())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(educationService.addEducation(userId, edu)));

        } catch (RuntimeException e) {
            // Retourne le message métier (ex: doublon détecté)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Lister les formations (triées par année DESC) ───────────────────────
    @GetMapping("/user/{userId}")
    @Operation(summary = "Lister les formations d'un freelancer (ordre anti-chronologique)")
    public ResponseEntity<List<EducationResponse>> getAll(@PathVariable Long userId) {
        List<EducationResponse> list = educationService.getMyEducations(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    // ─── Modifier une formation ──────────────────────────────────────────────
    @PutMapping("/{eduId}/user/{userId}")
    @Operation(summary = "Modifier une formation académique")
    public ResponseEntity<Object> update(
            @PathVariable Long eduId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateEducationRequest request) {

        try {
            return ResponseEntity.ok(
                    toResponse(educationService.updateEducation(eduId, userId, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── Supprimer une formation ─────────────────────────────────────────────
    @DeleteMapping("/{eduId}/user/{userId}")
    @Operation(summary = "Supprimer une formation académique")
    public ResponseEntity<Void> delete(
            @PathVariable Long eduId,
            @PathVariable Long userId) {
        educationService.deleteEducation(eduId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─── Mapper entité → DTO de réponse ─────────────────────────────────────
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