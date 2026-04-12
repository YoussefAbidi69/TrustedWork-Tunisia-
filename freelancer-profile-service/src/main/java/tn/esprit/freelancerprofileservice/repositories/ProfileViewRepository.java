package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.ProfileView;

import java.util.List;

public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    List<ProfileView> findByProfileId(Long profileId);

    long countByProfileId(Long profileId);
}