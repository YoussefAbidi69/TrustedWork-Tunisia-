package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.CareerSuggestion;

import java.util.List;

public interface CareerSuggestionRepository extends JpaRepository<CareerSuggestion, Long> {

    List<CareerSuggestion> findByFreelancerIdOrderByTotalScoreDesc(Long freelancerId);

    @Modifying
    @Query("DELETE FROM CareerSuggestion c WHERE c.freelancerId = :fid")
    void deleteByFreelancerId(@Param("fid") Long freelancerId);
}

