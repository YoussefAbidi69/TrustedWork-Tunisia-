package tn.esprit.freelancerprofileservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileViewAnalyticsResponse {

    private Long profileId;
    private long totalViews;
    private long uniqueViewers;
    private long viewsLast7Days;
}