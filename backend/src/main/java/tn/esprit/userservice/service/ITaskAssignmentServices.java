package tn.esprit.userservice.service;


import tn.esprit.userservice.entity.TaskAssignment;

import java.util.List;

public interface ITaskAssignmentServices {

    TaskAssignment assignTask(Long taskId, Long memberId);

    List<TaskAssignment> getAssignmentsByTask(Long taskId);

    List<TaskAssignment> getAssignmentsByMember(Long memberId);

    List<TaskAssignment> getAssignmentsByAgency(Long agencyId);

    TaskAssignment updateCompletionScore(Long assignmentId, Float completionScore);

    void deleteAssignment(Long assignmentId);

    TaskAssignment autoAssignTask(Long taskId, Long agencyId);

    TaskAssignment autoAssignBySkills(Long taskId, Long agencyId);

    TaskAssignment autoAssignSmart(Long taskId, Long agencyId);
}
