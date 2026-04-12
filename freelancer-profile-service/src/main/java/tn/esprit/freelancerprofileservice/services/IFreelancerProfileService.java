package tn.esprit.freelancerprofileservice.services;

import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;

import java.util.List;

public interface IFreelancerProfileService {
    FreelancerProfile createProfile(FreelancerProfile profile);
    FreelancerProfile getByUserId(Long userId);
    FreelancerProfile getById(Long profileId);
    FreelancerProfile updateProfile(Long userId, FreelancerProfile updates);
    List<FreelancerProfile> getAllPublicProfiles();
    List<FreelancerProfile> getRankingByRegion(String region);
    FreelancerProfile updateAvailability(Long userId, AvailabilityStatus status);
    void deleteProfile(Long userId);
}