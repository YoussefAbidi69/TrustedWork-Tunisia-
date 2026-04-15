package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.freelancerprofileservice.entities.WorkExperience;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Long> {


    List<WorkExperience> findByProfileIdOrderByIsCurrentDescStartDateDesc(Long profileId);


    long countByProfileId(Long profileId);


    Optional<WorkExperience> findByIdAndProfileId(Long id, Long profileId);


    boolean existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDate(
            Long profileId,
            String jobTitle,
            String company,
            LocalDate startDate
    );


    boolean existsByProfileIdAndJobTitleIgnoreCaseAndCompanyIgnoreCaseAndStartDateAndIdNot(
            Long profileId,
            String jobTitle,
            String company,
            LocalDate startDate,
            Long id
    );

    @Query("SELECT COALESCE(SUM(TIMESTAMPDIFF(MONTH, w.startDate, COALESCE(w.endDate, CURRENT_DATE))), 0) FROM WorkExperience w WHERE w.profile.id = :profileId")
    long sumMonthsByProfileId(@Param("profileId") Long profileId);
}