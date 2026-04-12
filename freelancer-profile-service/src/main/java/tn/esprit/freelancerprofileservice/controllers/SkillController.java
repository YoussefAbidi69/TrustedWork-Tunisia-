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
import tn.esprit.freelancerprofileservice.repositories.EndorsementRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion des compétences
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final ISkillService skillService;
    private final ISkillAuthenticityService authenticityService;
    private final ISkillGapService skillGapService;
    private final EndorsementRepository endorsementRepository;

    // POST /api/skills/user/{userId} — ajouter un skill
    @PostMapping("/user/{userId}")
    public ResponseEntity<SkillResponse> addSkill(
            @PathVariable Long userId,
            @Valid @RequestBody AddSkillRequest request) {

        Skill skill = Skill.builder()
                .name(request.getName())
                .examScore(request.getExamScore())
                .build();

        Skill saved = skillService.addSkill(userId, skill);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    // GET /api/skills/user/{userId} — mes skills
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SkillResponse>> getMySkills(@PathVariable Long userId) {
        List<SkillResponse> skills = skillService.getMySkills(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(skills);
    }

    // DELETE /api/skills/{skillId}/user/{userId} — supprimer un skill
    @DeleteMapping("/{skillId}/user/{userId}")
    public ResponseEntity<Void> deleteSkill(
            @PathVariable Long skillId,
            @PathVariable Long userId) {
        skillService.deleteSkill(skillId, userId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/skills/{skillId}/authenticity — score d'authenticité
    @GetMapping("/{skillId}/authenticity")
    public ResponseEntity<Double> getAuthenticityScore(@PathVariable Long skillId) {
        return ResponseEntity.ok(authenticityService.calculateAuthenticityScore(skillId));
    }

    // GET /api/skills/user/{userId}/gaps — skill gap analysis
    @GetMapping("/user/{userId}/gaps")
    public ResponseEntity<SkillGapResponse> getSkillGaps(@PathVariable Long userId) {
        return ResponseEntity.ok(skillGapService.detectSkillGaps(userId));
    }

    // Mapper entité → DTO response
    private SkillResponse toResponse(Skill s) {
        return SkillResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .level(s.getLevel())
                .authenticityScore(s.getAuthenticityScore())
                .examScore(s.getExamScore())
                .endorsementCount(endorsementRepository.countBySkillId(s.getId()))
                .build();
    }
}