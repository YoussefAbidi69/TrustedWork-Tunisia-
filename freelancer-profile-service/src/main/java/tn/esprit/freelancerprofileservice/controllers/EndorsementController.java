package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.AddEndorsementRequest;
import tn.esprit.freelancerprofileservice.dto.response.EndorsementResponse;
import tn.esprit.freelancerprofileservice.entities.Endorsement;
import tn.esprit.freelancerprofileservice.services.IEndorsementService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion des endorsements
 */
@RestController
@RequestMapping("/api/endorsements")
@RequiredArgsConstructor
public class EndorsementController {

    private final IEndorsementService endorsementService;

    // POST /api/endorsements/skill/{skillId}
    @PostMapping("/skill/{skillId}")
    public ResponseEntity<EndorsementResponse> addEndorsement(
            @PathVariable Long skillId,
            @Valid @RequestBody AddEndorsementRequest request) {

        Endorsement saved = endorsementService.addEndorsement(
                skillId, request.getEndorserId(), request.getComment());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    // GET /api/endorsements/skill/{skillId}
    @GetMapping("/skill/{skillId}")
    public ResponseEntity<List<EndorsementResponse>> getBySkill(@PathVariable Long skillId) {
        List<EndorsementResponse> endorsements = endorsementService
                .getEndorsementsBySkill(skillId)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(endorsements);
    }

    private EndorsementResponse toResponse(Endorsement e) {
        return EndorsementResponse.builder()
                .id(e.getId())
                .endorserId(e.getEndorserId())
                .comment(e.getComment())
                .endorsedAt(e.getEndorsedAt())
                .build();
    }
}