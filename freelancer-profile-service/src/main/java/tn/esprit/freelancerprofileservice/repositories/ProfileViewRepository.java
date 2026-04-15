package tn.esprit.freelancerprofileservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.freelancerprofileservice.entities.ProfileView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProfileViewRepository extends JpaRepository<ProfileView, Long> {

    List<ProfileView> findByProfileId(Long profileId);

    long countByProfileId(Long profileId);

    Optional<ProfileView> findTopByProfileIdAndViewerIdOrderByViewedAtDesc(Long profileId, Long viewerId);

    @Query("""
           select count(distinct pv.viewerId)
           from ProfileView pv
           where pv.profile.id = :profileId
             and pv.viewerId is not null
           """)
    long countDistinctViewersByProfileId(@Param("profileId") Long profileId);

    @Query("""
           select count(pv)
           from ProfileView pv
           where pv.profile.id = :profileId
             and pv.viewedAt >= :since
           """)
    long countViewsSince(@Param("profileId") Long profileId,
                         @Param("since") LocalDateTime since);
}