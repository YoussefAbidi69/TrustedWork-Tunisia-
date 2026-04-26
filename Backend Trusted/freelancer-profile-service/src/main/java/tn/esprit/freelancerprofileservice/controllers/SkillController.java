package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddSkillRequest;
import tn.esprit.freelancerprofileservice.dto.response.SkillGapResponse;
import tn.esprit.freelancerprofileservice.dto.response.SkillResponse;
import tn.esprit.freelancerprofileservice.entities.Skill;
import tn.esprit.freelancerprofileservice.services.ISkillAuthenticityService;
import tn.esprit.freelancerprofileservice.services.ISkillGapService;
import tn.esprit.freelancerprofileservice.services.ISkillService;

import java.util.List;

/**
 * Controller REST de gestion des compétences.
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final ISkillService skillService;
    private final ISkillAuthenticityService authenticityService;
    private final ISkillGapService skillGapService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<SkillResponse> addSkill(
            @PathVariable Long userId,
            @Valid @RequestBody AddSkillRequest request
    ) {
        Skill skill = Skill.builder()
                .name(request.getName())
                .category(request.getCategory())
                .examScore(request.getExamScore())
                .build();

        Skill savedSkill = skillService.addSkill(userId, skill);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(savedSkill));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SkillResponse>> getMySkills(@PathVariable Long userId) {
        List<SkillResponse> responses = skillService.getMySkills(userId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{skillId}/user/{userId}")
    public ResponseEntity<Void> deleteSkill(
            @PathVariable Long skillId,
            @PathVariable Long userId
    ) {
        skillService.deleteSkill(skillId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{skillId}/authenticity")
    public ResponseEntity<Double> getAuthenticityScore(@PathVariable Long skillId) {
        return ResponseEntity.ok(authenticityService.calculateAuthenticityScore(skillId));
    }

    @GetMapping("/user/{userId}/gaps")
    public ResponseEntity<SkillGapResponse> getSkillGaps(@PathVariable Long userId) {
        return ResponseEntity.ok(skillGapService.detectSkillGaps(userId));
    }

    private SkillResponse toResponse(Skill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .level(skill.getLevel())
                .authenticityScore(skill.getAuthenticityScore())
                .examScore(skill.getExamScore())
                .endorsementCount(skill.getEndorsementCount())
                .build();
    }


    /**
     * Mise à jour du score d'examen d'une compétence.
     * Recalcul automatique du score d'authenticité après mise à jour.
     */
    @PatchMapping("/{skillId}/user/{userId}/exam-score")
    public ResponseEntity<SkillResponse> updateExamScore(
            @PathVariable Long skillId,
            @PathVariable Long userId,
            @RequestBody java.util.Map<String, Double> body) {

        Double examScore = body.get("examScore");

        if (examScore == null || examScore < 0 || examScore > 100) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(skillService.updateExamScore(skillId, userId, examScore));
    }
}