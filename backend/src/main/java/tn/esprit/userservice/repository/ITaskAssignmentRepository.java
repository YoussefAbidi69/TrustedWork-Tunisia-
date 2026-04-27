package tn.esprit.userservice.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.userservice.entity.TaskAssignment;

import java.util.List;
import java.util.Optional;

public interface ITaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

  List<TaskAssignment> findByTaskId(Long taskId);

  List<TaskAssignment> findByMemberId(Long memberId);

  List<TaskAssignment> findByMemberAgencyId(Long agencyId);

  Optional<TaskAssignment> findByTaskIdAndMemberId(Long taskId, Long memberId);
}