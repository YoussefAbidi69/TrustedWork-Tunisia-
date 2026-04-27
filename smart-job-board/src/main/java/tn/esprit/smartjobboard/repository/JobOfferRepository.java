package tn.esprit.smartjobboard.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.smartjobboard.entity.JobOffer;
import tn.esprit.smartjobboard.entity.JobOfferStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {

    long countByClientIdAndCreatedAtAfter(Long clientId, LocalDateTime after);

    @Query("SELECT COUNT(j) FROM JobOffer j WHERE j.clientId = :clientId AND LOWER(j.title) = LOWER(:title) AND j.createdAt >= :since AND (:excludeId IS NULL OR j.id <> :excludeId)")
    long countDuplicateTitleSince(@Param("clientId") Long clientId,
                                  @Param("title") String title,
                                  @Param("since") LocalDateTime since,
                                  @Param("excludeId") Long excludeId);

    List<JobOffer> findByStatusAndPublishedAtAfterAndOpportunityAgentProcessedAtIsNull(
            JobOfferStatus status, LocalDateTime publishedAfter);

    List<JobOffer> findByStatus(JobOfferStatus status);

    long countByStatus(JobOfferStatus status);

    @Query("SELECT COUNT(j) FROM JobOffer j WHERE j.category = :category AND j.status = :st AND j.publishedAt >= :since")
    long countPublishedInCategorySince(@Param("category") String category,
                                       @Param("st") JobOfferStatus status,
                                       @Param("since") LocalDateTime since);

    @Query(value = """
            SELECT LOWER(TRIM(es.skill)) AS sk, COUNT(*) AS cnt
            FROM job_offer_extracted_skills es
            INNER JOIN job_offers j ON j.id = es.job_offer_id
            WHERE j.status = 'PUBLISHED'
              AND j.published_at IS NOT NULL
              AND j.published_at >= :from
              AND j.published_at < :to
            GROUP BY LOWER(TRIM(es.skill))
            """, nativeQuery = true)
    List<Object[]> countExtractedSkillsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(j) FROM JobOffer j WHERE " +
            "LOWER(j.description) LIKE %:skill1% AND " +
            "LOWER(j.description) LIKE %:skill2% AND " +
            "j.status = 'PUBLISHED'")
    long countJobsWithBothSkills(@Param("skill1") String skill1,
                                 @Param("skill2") String skill2);
}
