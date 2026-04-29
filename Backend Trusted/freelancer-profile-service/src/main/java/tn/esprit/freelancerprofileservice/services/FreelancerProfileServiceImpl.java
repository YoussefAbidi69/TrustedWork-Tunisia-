package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.exceptions.DuplicateResourceException;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;

import java.util.List;

/**
 * Implémentation du service principal de gestion des profils freelancer
 */
@Service
@RequiredArgsConstructor
public class FreelancerProfileServiceImpl implements IFreelancerProfileService {

    private final FreelancerProfileRepository profileRepository;

    @Override
    public FreelancerProfile createProfile(FreelancerProfile profile) {
        if (profileRepository.existsByUserId(profile.getUserId())) {
            throw new DuplicateResourceException("Profil déjà existant pour userId: " + profile.getUserId());
        }
        profile.setCompletenessScore(0);
        profile.setTotalViews(0);
        return profileRepository.save(profile);
    }

    @Override
    public FreelancerProfile getByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", userId));
    }

    @Override
    public FreelancerProfile getById(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", profileId));
    }

    @Override
    public FreelancerProfile updateProfile(Long userId, FreelancerProfile updates) {
        FreelancerProfile existing = getByUserId(userId);
        existing.setHeadline(updates.getHeadline());
        existing.setBio(updates.getBio());
        existing.setAvatarUrl(updates.getAvatarUrl());
        existing.setHourlyRate(updates.getHourlyRate());
        existing.setAvailabilityStatus(updates.getAvailabilityStatus());
        existing.setVisibility(updates.getVisibility());
        existing.setProjectType(updates.getProjectType());
        existing.setRegion(updates.getRegion());
        return profileRepository.save(existing);
    }

    @Override
    public List<FreelancerProfile> getAllPublicProfiles() {
        return profileRepository.findAllPublicProfiles();
    }

    @Override
    public List<FreelancerProfile> getRankingByRegion(String region) {
        return profileRepository.findByRegionOrderByCompletenessScoreDesc(region);
    }

    @Override
    public List<FreelancerProfile> searchProfiles(String region, AvailabilityStatus availability, Double minRate, Double maxRate) {
        return profileRepository.searchProfiles(region, availability, minRate, maxRate);
    }

    @Override
    public FreelancerProfile updateAvailability(Long userId, AvailabilityStatus status) {
        FreelancerProfile profile = getByUserId(userId);
        profile.setAvailabilityStatus(status);
        return profileRepository.save(profile);
    }

    @Override
    public void deleteProfile(Long userId) {
        FreelancerProfile profile = getByUserId(userId);
        profileRepository.delete(profile);
    }
}