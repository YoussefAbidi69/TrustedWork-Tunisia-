package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.freelancerprofileservice.entities.Education;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {

    List<Education> findByProfileId(Long profileId);
}