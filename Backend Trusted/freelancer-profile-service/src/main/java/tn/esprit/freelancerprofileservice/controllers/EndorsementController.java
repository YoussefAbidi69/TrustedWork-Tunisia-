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

/**
 * Controller REST de gestion des endorsements.
 */
@RestController
@RequestMapping("/api/endorsements")
@RequiredArgsConstructor
public class EndorsementController {

    private final IEndorsementService endorsementService;

    @PostMapping("/skill/{skillId}")
    public ResponseEntity<EndorsementResponse> addEndorsement(
            @PathVariable Long skillId,
            @Valid @RequestBody AddEndorsementRequest request
    ) {
        Endorsement savedEndorsement = endorsementService.addEndorsement(
                skillId,
                request.getEndorserId(),
                request.getComment()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(savedEndorsement));
    }

    @GetMapping("/skill/{skillId}")
    public ResponseEntity<List<EndorsementResponse>> getBySkill(@PathVariable Long skillId) {
        List<EndorsementResponse> responses = endorsementService.getEndorsementsBySkill(skillId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/skill/{skillId}/count")
    public ResponseEntity<Long> countBySkill(@PathVariable Long skillId) {
        return ResponseEntity.ok(endorsementService.countEndorsements(skillId));
    }

    private EndorsementResponse toResponse(Endorsement endorsement) {
        return EndorsementResponse.builder()
                .id(endorsement.getId())
                .endorserId(endorsement.getEndorserId())
                .comment(endorsement.getComment())
                .endorsedAt(endorsement.getEndorsedAt())
                .build();
    }
}