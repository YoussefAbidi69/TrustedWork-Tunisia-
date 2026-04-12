package tn.esprit.freelancerprofileservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.entities.ProfileView;
import tn.esprit.freelancerprofileservice.repositories.FreelancerProfileRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileViewRepository;

/**
 * Implémentation du service d'analytics de visites
 */
@Service
@RequiredArgsConstructor
public class ProfileViewServiceImpl implements IProfileViewService {

    private final ProfileViewRepository viewRepository;
    private final FreelancerProfileRepository profileRepository;

    @Override
    public void recordView(Long profileId, Long viewerId) {
        FreelancerProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profil introuvable"));
        ProfileView view = ProfileView.builder()
                .profile(profile)
                .viewerId(viewerId)
                .build();
        viewRepository.save(view);
        profile.setTotalViews(profile.getTotalViews() + 1);
        profileRepository.save(profile);
    }

    @Override
    public long getTotalViews(Long profileId) {
        return viewRepository.countByProfileId(profileId);
    }
}