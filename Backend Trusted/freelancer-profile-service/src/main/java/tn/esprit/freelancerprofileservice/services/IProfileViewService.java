package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.dto.response.ProfileViewAnalyticsResponse;

public interface IProfileViewService {

    void recordView(Long profileId, Long viewerId);

    long getTotalViews(Long profileId);

    ProfileViewAnalyticsResponse getAnalytics(Long profileId);
}