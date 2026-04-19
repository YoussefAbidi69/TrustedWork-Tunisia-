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

    /** All agencies this user is a member of */
    private List<AgencyMembershipSummary> memberships;

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
