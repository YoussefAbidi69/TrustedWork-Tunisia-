package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.AgencyJoinRequestDto;
import tn.esprit.userservice.dto.AgencyJoinRequestResponseDto;
import tn.esprit.userservice.dto.AgencyJoinRequestUpdateDto;
import tn.esprit.userservice.entity.AgencyJoinRequest;
import tn.esprit.userservice.entity.JoinRequestStatus;
import tn.esprit.userservice.service.IAgencyJoinRequestService;

import java.util.List;

/**
 * Handles the freelancer-initiated "join request" flow:
 *   POST   /agencies/{agencyId}/requests           — freelancer sends request
 *   GET    /agencies/{agencyId}/requests?ownerId=X — owner views pending requests
 *   PATCH  /agencies/{agencyId}/requests/{id}      — owner accepts/declines
 *   DELETE /agencies/{agencyId}/requests/{id}      — freelancer cancels own request
 *   GET    /agencies/requests/my/{userId}           — freelancer sees their own requests
 */
@RestController
@RequestMapping("/agencies")
@RequiredArgsConstructor
public class AgencyJoinRequestController {

    private final IAgencyJoinRequestService joinRequestService;

    // ── Helper mapper (inline to avoid a separate mapper class) ──────────────
    private AgencyJoinRequestResponseDto toDto(AgencyJoinRequest r) {
        return AgencyJoinRequestResponseDto.builder()
                .id(r.getId())
                .agencyId(r.getAgency() != null ? r.getAgency().getId() : null)
                .agencyName(r.getAgency() != null ? r.getAgency().getName() : null)
                .requesterId(r.getRequester() != null ? r.getRequester().getId() : null)
                .requesterFirstName(r.getRequester() != null ? r.getRequester().getFirstName() : null)
                .requesterLastName(r.getRequester() != null ? r.getRequester().getLastName() : null)
                .requesterEmail(r.getRequester() != null ? r.getRequester().getEmail() : null)
                .requesterPhoto(r.getRequester() != null ? r.getRequester().getPhoto() : null)
                .requesterSkills(r.getRequester() != null ? r.getRequester().getSkills() : null)
                .requesterHeadline(r.getRequester() != null ? r.getRequester().getHeadline() : null)
                .status(r.getStatus())
                .message(r.getMessage())
                .requestedAt(r.getRequestedAt())
                .respondedAt(r.getRespondedAt())
                .build();
    }

    /**
     * AGC-05: Freelancer sends a join request to an agency.
     * POST /agencies/{agencyId}/requests
     * Body: { requesterId, message }
     */
    @PostMapping("/{agencyId}/requests")
    public ResponseEntity<?> sendJoinRequest(
            @PathVariable Long agencyId,
            @RequestBody AgencyJoinRequestDto dto) {
        try {
            AgencyJoinRequest saved = joinRequestService.sendJoinRequest(
                    agencyId, dto.getRequesterId(), dto.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * AGC-02: Owner retrieves join requests for their agency.
     * GET /agencies/{agencyId}/requests?ownerId=X&status=PENDING
     */
    @GetMapping("/{agencyId}/requests")
    public ResponseEntity<?> getJoinRequests(
            @PathVariable Long agencyId,
            @RequestParam Long ownerId,
            @RequestParam(required = false) JoinRequestStatus status) {
        try {
            List<AgencyJoinRequestResponseDto> result = joinRequestService
                    .getRequestsByAgency(agencyId, ownerId, status)
                    .stream().map(this::toDto).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * AGC-02: Owner accepts or declines a join request.
     * PATCH /agencies/{agencyId}/requests/{requestId}
     * Body: { status: "ACCEPTED" | "DECLINED" }
     */
    @PatchMapping("/{agencyId}/requests/{requestId}")
    public ResponseEntity<?> respondToRequest(
            @PathVariable Long agencyId,
            @PathVariable Long requestId,
            @RequestParam Long ownerId,
            @RequestBody AgencyJoinRequestUpdateDto dto) {
        try {
            AgencyJoinRequest updated = joinRequestService.respondToRequest(requestId, ownerId, dto.getStatus());
            return ResponseEntity.ok(toDto(updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * Freelancer cancels their own pending request.
     * DELETE /agencies/{agencyId}/requests/{requestId}?userId=X
     */
    @DeleteMapping("/{agencyId}/requests/{requestId}")
    public ResponseEntity<?> cancelRequest(
            @PathVariable Long agencyId,
            @PathVariable Long requestId,
            @RequestParam Long userId) {
        try {
            joinRequestService.cancelRequest(requestId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    /**
     * Freelancer views all their own join requests (all agencies).
     * GET /agencies/requests/my/{userId}
     */
    @GetMapping("/requests/my/{userId}")
    public List<AgencyJoinRequestResponseDto> getMyRequests(@PathVariable Long userId) {
        return joinRequestService.getRequestsByUser(userId)
                .stream().map(this::toDto).toList();
    }
}
