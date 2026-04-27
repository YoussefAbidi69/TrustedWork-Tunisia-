package tn.esprit.userservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.AgencyMember;
import tn.esprit.userservice.entity.MemberStatus;
import tn.esprit.userservice.entity.Task;
import tn.esprit.userservice.entity.TaskAssignment;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.ITaskAssignmentRepository;
import tn.esprit.userservice.repository.ITaskRepository;
import tn.esprit.userservice.service.ITaskAssignmentServices;
import tn.esprit.userservice.service.IEmailService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskAssignmentServiceImpl implements ITaskAssignmentServices {

    private final ITaskAssignmentRepository taskAssignmentRepository;
    private final ITaskRepository taskRepository;
    private final IAgencyMemberRepository agencyMemberRepository;
    private final IEmailService emailService;

    @Override
    public TaskAssignment assignTask(Long taskId, Long memberId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        AgencyMember member = agencyMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Agency member not found"));

        if (taskAssignmentRepository.findByTaskIdAndMemberId(taskId, memberId).isPresent()) {
            throw new RuntimeException("Task is already assigned to this member");
        }

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .member(member)
                .assignedAt(LocalDateTime.now())
                .completionScore(0f)
                .build();

        return taskAssignmentRepository.save(assignment);
    }

    @Override
    public List<TaskAssignment> getAssignmentsByTask(Long taskId) {
        return taskAssignmentRepository.findByTaskId(taskId);
    }

    @Override
    public List<TaskAssignment> getAssignmentsByMember(Long memberId) {
        return taskAssignmentRepository.findByMemberId(memberId);
    }

    @Override
    public List<TaskAssignment> getAssignmentsByAgency(Long agencyId) {
        return taskAssignmentRepository.findByMemberAgencyId(agencyId);
    }

    @Override
    public TaskAssignment updateCompletionScore(Long assignmentId, Float completionScore) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Task assignment not found"));

        assignment.setCompletionScore(completionScore);

        if (completionScore != null && completionScore >= 100f) {
            assignment.setCompletedAt(LocalDateTime.now());
        }

        return taskAssignmentRepository.save(assignment);
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        taskAssignmentRepository.deleteById(assignmentId);
    }

    @Override
    public TaskAssignment autoAssignTask(Long taskId, Long agencyId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        List<AgencyMember> members = agencyMemberRepository.findByAgencyIdAndStatus(agencyId, MemberStatus.ACTIVE);

        if (members.isEmpty()) {
            throw new RuntimeException("No active members in this agency");
        }

        AgencyMember selectedMember = null;
        long minTasks = Long.MAX_VALUE;

        for (AgencyMember member : members) {
            long taskCount = taskAssignmentRepository.findByMemberId(member.getId()).size();

            if (taskCount < minTasks) {
                minTasks = taskCount;
                selectedMember = member;
            }
        }

        if (selectedMember == null) {
            throw new RuntimeException("No suitable member found");
        }

        if (taskAssignmentRepository.findByTaskIdAndMemberId(taskId, selectedMember.getId()).isPresent()) {
            throw new RuntimeException("Task is already assigned to this member");
        }

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .member(selectedMember)
                .completionScore(0f)
                .assignedAt(LocalDateTime.now())
                .build();

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        sendNotification(task, selectedMember);
        return saved;
    }

    @Override
    public TaskAssignment autoAssignBySkills(Long taskId, Long agencyId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        List<AgencyMember> members = agencyMemberRepository.findByAgencyIdAndStatus(agencyId, MemberStatus.ACTIVE);

        if (members.isEmpty()) {
            throw new RuntimeException("No active members in this agency");
        }

        AgencyMember bestMatch = null;
        int maxScore = -1;

        for (AgencyMember member : members) {
            int score = calculateSkillMatchScore(member.getSkills(), task.getRequiredSkills());

            if (score > maxScore) {
                maxScore = score;
                bestMatch = member;
            }
        }

        if (bestMatch == null) {
            throw new RuntimeException("No suitable member found");
        }

        if (taskAssignmentRepository.findByTaskIdAndMemberId(taskId, bestMatch.getId()).isPresent()) {
            throw new RuntimeException("Task is already assigned to this member");
        }

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .member(bestMatch)
                .completionScore(0f)
                .assignedAt(LocalDateTime.now())
                .build();

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        sendNotification(task, bestMatch);
        return saved;
    }

    @Override
    public TaskAssignment autoAssignSmart(Long taskId, Long agencyId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        List<AgencyMember> members = agencyMemberRepository.findByAgencyIdAndStatus(agencyId, MemberStatus.ACTIVE);

        if (members.isEmpty()) {
            throw new RuntimeException("No active members in this agency");
        }

        AgencyMember bestMember = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (AgencyMember member : members) {
            int skillScore = calculateSkillMatchScore(
                    member.getSkills(),
                    task.getRequiredSkills()
            );

            long workload = taskAssignmentRepository.findByMemberId(member.getId()).size();

            double finalScore = (skillScore * 2.0) - workload;

            if (finalScore > bestScore) {
                bestScore = finalScore;
                bestMember = member;
            }
        }

        if (bestMember == null) {
            throw new RuntimeException("No suitable member found");
        }

        if (taskAssignmentRepository.findByTaskIdAndMemberId(taskId, bestMember.getId()).isPresent()) {
            throw new RuntimeException("Task is already assigned to this member");
        }

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .member(bestMember)
                .completionScore(0f)
                .assignedAt(LocalDateTime.now())
                .build();

        TaskAssignment saved = taskAssignmentRepository.save(assignment);
        sendNotification(task, bestMember);
        return saved;
    }

    private int calculateSkillMatchScore(String memberSkills, String requiredSkills) {
        if (memberSkills == null || memberSkills.trim().isEmpty()) {
            return 0;
        }

        if (requiredSkills == null || requiredSkills.trim().isEmpty()) {
            return 0;
        }

        List<String> memberList = Arrays.stream(memberSkills.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(skill -> !skill.isEmpty())
                .toList();

        List<String> requiredList = Arrays.stream(requiredSkills.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(skill -> !skill.isEmpty())
                .toList();

        int score = 0;

        for (String skill : requiredList) {
            if (memberList.contains(skill)) {
                score++;
            }
        }

        return score;
    }

    private void sendNotification(Task task, AgencyMember member) {
        emailService.sendAutoAssignTaskEmail(
            member.getUser().getEmail(),
            member.getUser().getFirstName(),
            task.getAgency().getName(),
            task.getTitle(),
            task.getProject().getName(),
            task.getPriority().name(),
            task.getDueDate() != null ? task.getDueDate().toString() : null,
            task.getDescription()
        );
    }
}