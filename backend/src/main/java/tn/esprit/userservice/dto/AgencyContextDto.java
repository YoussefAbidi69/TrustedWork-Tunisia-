package tn.esprit.userservice.dto;

import lombok.*;
import java.util.List;

/**
 * Returned by GET /agencies/my-context/{userId}
 * Drives the frontend "Create vs Enter" decision.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyContextDto {

    /** true  → show "Select Agency" or auto-enter  */
    /** false → show "Create Agency" button only     */
    private boolean hasMemberships;

    /** true → user is LEAD of at least one agency (show "Mon Agence" in sidebar) */
    private boolean ownsAnAgency;

    /** All agencies this user is a member of */
    private List<AgencyMembershipSummary> memberships;

    private int pendingInvitationCount;
    private List<tn.esprit.userservice.dto.AgencyInvitationResponseDto> pendingInvitations;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencyMembershipSummary {
        private Long agencyId;
        private String agencyName;
        private String logoUrl;
        private String role;   // "LEAD" | "MEMBER"
        private String status; // "ACTIVE" | "INACTIVE"
        private String joinedAt;
    }
}
