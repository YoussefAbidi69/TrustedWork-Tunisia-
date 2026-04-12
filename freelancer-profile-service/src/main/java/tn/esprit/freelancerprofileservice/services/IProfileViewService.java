package tn.esprit.freelancerprofileservice.services;

public interface IProfileViewService {
    void recordView(Long profileId, Long viewerId);
    long getTotalViews(Long profileId);
}