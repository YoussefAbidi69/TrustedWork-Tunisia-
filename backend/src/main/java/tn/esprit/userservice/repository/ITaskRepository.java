package tn.esprit.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskPriority;
import tn.esprit.userservice.entity.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ITaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAgencyId(Long agencyId);

    List<Task> findByAgencyIdAndProjectId(Long agencyId, Long projectId);

    List<Task> findByProjectId(Long projectId);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    List<Task> findByProjectIdAndPriority(Long projectId, TaskPriority priority);

    List<Task> findByDueDateBeforeAndStatusNot(LocalDateTime date, TaskStatus status);

    long countByAgencyId(Long agencyId);
    long countByAgencyIdAndStatus(Long agencyId, TaskStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t WHERE t.agency.id = :agencyId AND t.assignedMember IS NULL " +
           "ORDER BY CASE t.priority " +
           "WHEN tn.esprit.userservice.entity.TaskPriority.URGENTE THEN 4 " +
           "WHEN tn.esprit.userservice.entity.TaskPriority.HAUTE THEN 3 " +
           "WHEN tn.esprit.userservice.entity.TaskPriority.MOYENNE THEN 2 " +
           "WHEN tn.esprit.userservice.entity.TaskPriority.FAIBLE THEN 1 " +
           "ELSE 0 END DESC")
    List<Task> findUnassignedTasksByPriority(Long agencyId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Task t " +
           "LEFT JOIN FETCH t.assignedMember am " +
           "LEFT JOIN FETCH am.user u " +
           "LEFT JOIN FETCH t.project tp " +
           "WHERE t.agency.id = :agencyId " +
           "AND (:query IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.updatedAt DESC")
    org.springframework.data.domain.Page<Task> searchByAgencyAndTitle(@org.springframework.data.repository.query.Param("agencyId") Long agencyId, @org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);
}