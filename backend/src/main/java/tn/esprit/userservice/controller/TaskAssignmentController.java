package tn.esprit.userservice.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.TaskAssignmentRequestDto;
import tn.esprit.userservice.dto.TaskAssignmentResponseDto;
import tn.esprit.userservice.dto.TaskAssignmentUpdateDto;
import tn.esprit.userservice.entity.TaskAssignment;
import tn.esprit.userservice.mapper.TaskAssignmentMapper;
import tn.esprit.userservice.service.ITaskAssignmentServices;

import java.util.List;

@RestController
@RequestMapping("/task-assignments")
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final ITaskAssignmentServices taskAssignmentService;
    private final TaskAssignmentMapper taskAssignmentMapper;

    // ASSIGN MANUALLY
    @PostMapping("/assign")
    public TaskAssignmentResponseDto assignTask(@RequestBody TaskAssignmentRequestDto dto) {
        TaskAssignment assignment = taskAssignmentService.assignTask(dto.getTaskId(), dto.getMemberId());
        return taskAssignmentMapper.toResponseDto(assignment);
    }

    // WORKLOAD BALANCER
    @PostMapping("/auto/workload")
    public TaskAssignmentResponseDto autoAssignWorkload(
            @RequestParam Long taskId,
            @RequestParam Long agencyId
    ) {
        TaskAssignment assignment = taskAssignmentService.autoAssignTask(taskId, agencyId);
        return taskAssignmentMapper.toResponseDto(assignment);
    }

    // SKILL MATCHING
    @PostMapping("/auto/skills")
    public TaskAssignmentResponseDto autoAssignSkills(
            @RequestParam Long taskId,
            @RequestParam Long agencyId
    ) {
        TaskAssignment assignment = taskAssignmentService.autoAssignBySkills(taskId, agencyId);
        return taskAssignmentMapper.toResponseDto(assignment);
    }

    // SMART AI (SKILL + WORKLOAD)
    @PostMapping("/auto/smart")
    public TaskAssignmentResponseDto autoAssignSmart(
            @RequestParam Long taskId,
            @RequestParam Long agencyId
    ) {
        TaskAssignment assignment = taskAssignmentService.autoAssignSmart(taskId, agencyId);
        return taskAssignmentMapper.toResponseDto(assignment);
    }

    // GET BY TASK
    @GetMapping("/task/{taskId}")
    public List<TaskAssignmentResponseDto> getAssignmentsByTask(@PathVariable Long taskId) {
        return taskAssignmentService.getAssignmentsByTask(taskId)
                .stream()
                .map(taskAssignmentMapper::toResponseDto)
                .toList();
    }

    // GET BY MEMBER
    @GetMapping("/member/{memberId}")
    public List<TaskAssignmentResponseDto> getAssignmentsByMember(@PathVariable Long memberId) {
        return taskAssignmentService.getAssignmentsByMember(memberId)
                .stream()
                .map(taskAssignmentMapper::toResponseDto)
                .toList();
    }

    // UPDATE COMPLETION SCORE
    @PutMapping("/{assignmentId}")
    public TaskAssignmentResponseDto updateCompletionScore(
            @PathVariable Long assignmentId,
            @RequestBody TaskAssignmentUpdateDto dto
    ) {
        TaskAssignment updatedAssignment =
                taskAssignmentService.updateCompletionScore(assignmentId, dto.getCompletionScore());

        return taskAssignmentMapper.toResponseDto(updatedAssignment);
    }

    // DELETE ASSIGNMENT
    @DeleteMapping("/{assignmentId}")
    public void deleteAssignment(@PathVariable Long assignmentId) {
        taskAssignmentService.deleteAssignment(assignmentId);
    }
}