package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tn.esprit.smartjobboard.entity.FreelancerProfile;

import java.util.List;
import java.util.Optional;

public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Long> {

    Optional<FreelancerProfile> findByUserId(Long userId);

    @Query("SELECT DISTINCT f FROM FreelancerProfile f JOIN f.skills s WHERE SIZE(f.skills) > 0")
    List<FreelancerProfile> findAllWithSkills();
}
