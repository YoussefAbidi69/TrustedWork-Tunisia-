package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.TaskUpdateDto;
import tn.esprit.userservice.entity.*;
import tn.esprit.userservice.repository.IAgencyMemberRepository;
import tn.esprit.userservice.repository.ITaskRepository;
import tn.esprit.userservice.repository.ITeamProjectRepository;
import tn.esprit.userservice.repository.IAgencyRepository;
import tn.esprit.userservice.repository.ITaskAssignmentRepository;

import java.util.List;
import tn.esprit.userservice.service.IEmailService;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskServiceImpl implements ITaskServices {

    private final ITaskRepository taskRepository;
    private final ITeamProjectRepository projectRepository;
    private final IAgencyMemberRepository agencyMemberRepository;
    private final IAgencyRepository agencyRepository;
    private final ITaskAssignmentRepository taskAssignmentRepository;
    private final IEmailService emailService;

    @Override
    public List<Task> getTasksByAgency(Long agencyId) {
        return taskRepository.findByAgencyId(agencyId);
    }

    @Override
    public List<Task> getTasksByProject(Long agencyId, Long projectId) {
        return taskRepository.findByAgencyIdAndProjectId(agencyId, projectId);
    }

    private AgencyMember getLead(Long agencyId, Long requesterId) {
        // 1. Try to find the member record
        java.util.Optional<AgencyMember> requesterOpt = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, requesterId);
        
        if (requesterOpt.isPresent()) {
            AgencyMember requester = requesterOpt.get();
            if (requester.getRole() != MemberRole.LEAD) {
                throw new RuntimeException("Seul un LEAD peut modifier les tâches");
            }
            return requester;
        }

        // 2. Fallback: If not found, check if it's the official agency owner
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agence introuvable (ID: " + agencyId + ")"));

        if (agency.getCreatedBy() != null) {
            if (agency.getCreatedBy().getId().equals(requesterId)) {
                AgencyMember autoLead = AgencyMember.builder()
                        .agency(agency)
                        .user(agency.getCreatedBy())
                        .role(MemberRole.LEAD)
                        .status(MemberStatus.ACTIVE)
                        .workloadScore(0f)
                        .build();
                return agencyMemberRepository.save(autoLead);
            }
        }

        throw new RuntimeException("L'utilisateur n'est pas membre de cette agence (RequesterID: " + requesterId + ")");
    }

    private void syncAssignment(Task task, AgencyMember member) {
        // Clear existing assignments for this task in the junction table 
        // (assuming single assignee UI logic for now)
        taskAssignmentRepository.findByTaskId(task.getId()).forEach(taskAssignmentRepository::delete);

        if (member != null) {
            TaskAssignment assignment = TaskAssignment.builder()
                    .task(task)
                    .member(member)
                    .assignedAt(java.time.LocalDateTime.now())
                    .completionScore(0f)
                    .build();
            taskAssignmentRepository.save(assignment);
        }
    }

    @Override
    @Transactional
    public Task createTask(Long agencyId, Long projectId, Long requesterId, Long assignedMemberId, Task task) {
        AgencyMember lead = getLead(agencyId, requesterId);
        
        TeamProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
                
        if (!project.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Project does not belong to this agency");
        }

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        task.setProject(project);
        task.setAgency(agency);
        task.setCreatedBy(lead);

        if (assignedMemberId != null) {
            AgencyMember assignedMember = agencyMemberRepository.findById(assignedMemberId)
                    .orElseThrow(() -> new RuntimeException("Assigned member not found"));
            if (!assignedMember.getAgency().getId().equals(agencyId)) {
                throw new RuntimeException("Assigned member must belong to the same agency");
            }
            task.setAssignedMember(assignedMember);
        }

        Task savedTask = taskRepository.save(task);
        if (savedTask.getAssignedMember() != null) {
            syncAssignment(savedTask, savedTask.getAssignedMember());
        }
        return savedTask;
    }

    @Override
    @Transactional
    public Task updateTask(Long agencyId, Long taskId, Long requesterId, TaskUpdateDto dto) {
        getLead(agencyId, requesterId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Task does not belong to this agency");
        }

        if (dto.getProjectId() != null && !dto.getProjectId().equals(task.getProject().getId())) {
            TeamProject project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            if (!project.getAgency().getId().equals(agencyId)) {
                throw new RuntimeException("Project does not belong to this agency");
            }
            task.setProject(project);
        }

        if (dto.getAssignedMemberId() != null) {
            AgencyMember assignedMember = agencyMemberRepository.findById(dto.getAssignedMemberId())
                    .orElseThrow(() -> new RuntimeException("Assigned member not found"));
            if (!assignedMember.getAgency().getId().equals(agencyId)) {
                throw new RuntimeException("Assigned member must belong to the same agency");
            }
            task.setAssignedMember(assignedMember);
            syncAssignment(task, assignedMember);
        }

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task updateTaskStatus(Long agencyId, Long taskId, Long requesterId, TaskStatus status) {
        // Find task first
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Task does not belong to this agency");
        }

        // Check permissions
        java.util.Optional<AgencyMember> requesterOpt = agencyMemberRepository.findByAgencyIdAndUserId(agencyId, requesterId);
        boolean isLead = false;
        
        if (requesterOpt.isPresent()) {
            isLead = requesterOpt.get().getRole() == MemberRole.LEAD;
        } else {
            // Fallback for owner even if record missing
            Agency agency = agencyRepository.findById(agencyId).orElseThrow();
            if (agency.getCreatedBy() != null && agency.getCreatedBy().getId().equals(requesterId)) {
                isLead = true;
            }
        }

        boolean isAssigned = task.getAssignedMember() != null && 
                             task.getAssignedMember().getUser().getId().equals(requesterId);

        if (!isLead && !isAssigned) {
            throw new RuntimeException("Vous ne pouvez modifier le statut que des tâches qui vous sont assignées");
        }

        task.setStatus(status);
        
        if (status == TaskStatus.TERMINE || status == TaskStatus.ANNULE) {
            if (status == TaskStatus.TERMINE) {
                taskAssignmentRepository.findByTaskId(task.getId()).forEach(asgn -> {
                    asgn.setCompletedAt(java.time.LocalDateTime.now());
                    asgn.setCompletionScore(100f);
                    taskAssignmentRepository.save(asgn);
                });
            }
            
            // Trigger auto-assignment ONLY for the member who just finished/cancelled
            autoAssignTasks(agencyId, task.getAssignedMember());
        }
        
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long agencyId, Long taskId, Long requesterId) {
        getLead(agencyId, requesterId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getAgency().getId().equals(agencyId)) {
            throw new RuntimeException("Task does not belong to this agency");
        }

        taskRepository.delete(task);
    }

    @Transactional
    public void autoAssignTasks(Long agencyId, AgencyMember member) {
        if (member == null) return;
        
        System.out.println("[AutoAssign] Checking for unassigned task for member: " + member.getUser().getFirstName());
        
        // 1. Verify if the member is REALLY idle (no other tasks except completed/cancelled)
        long pendingTasks = taskRepository.findByAgencyId(agencyId).stream()
            .filter(t -> t.getAssignedMember() != null && t.getAssignedMember().getId().equals(member.getId()))
            .filter(t -> t.getStatus() != TaskStatus.TERMINE && t.getStatus() != TaskStatus.ANNULE)
            .count();
            
        if (pendingTasks > 0) {
            System.out.println("[AutoAssign] Member " + member.getUser().getFirstName() + " still has active tasks. Skipping auto-assign.");
            return;
        }

        // 2. Find the highest priority unassigned task
        List<Task> orphans = taskRepository.findUnassignedTasksByPriority(agencyId);
        if (orphans.isEmpty()) return;

        Task taskToAssign = orphans.get(0); // Take the most urgent one

        // 3. Assign
        taskToAssign.setAssignedMember(member);
        taskToAssign.setStatus(TaskStatus.A_FAIRE);
        taskRepository.save(taskToAssign);
        syncAssignment(taskToAssign, member);

        System.out.println("[AutoAssign] Task '" + taskToAssign.getTitle() + "' automatically moved from Backlog to 'À Faire' and assigned to " + member.getUser().getFirstName());

        // Send email
        emailService.sendAutoAssignTaskEmail(
            member.getUser().getEmail(),
            member.getUser().getFirstName(),
            taskToAssign.getAgency().getName(),
            taskToAssign.getTitle(),
            taskToAssign.getProject().getName(),
            taskToAssign.getPriority().name(),
            taskToAssign.getDueDate() != null ? taskToAssign.getDueDate().toString() : null,
            taskToAssign.getDescription()
        );
    }
}