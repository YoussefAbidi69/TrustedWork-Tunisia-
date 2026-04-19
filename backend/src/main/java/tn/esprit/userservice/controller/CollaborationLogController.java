package tn.esprit.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.userservice.dto.CollaborationLogRequestDto;
import tn.esprit.userservice.dto.CollaborationLogResponseDto;
import tn.esprit.userservice.entity.CollaborationLog;
import tn.esprit.userservice.mapper.CollaborationLogMapper;
import tn.esprit.userservice.service.ICollaborationLogServices;

import java.util.List;

@RestController
@RequestMapping("/collaboration-logs")
@RequiredArgsConstructor
public class CollaborationLogController {

    private final ICollaborationLogServices collaborationLogService;
    private final CollaborationLogMapper collaborationLogMapper;

    // SEND MESSAGE TO AGENCY
    @PostMapping("/agency/{agencyId}")
    public CollaborationLogResponseDto sendMessage(
            @PathVariable Long agencyId,
            @RequestBody CollaborationLogRequestDto dto
    ) {
        CollaborationLog log = collaborationLogMapper.toEntity(dto);
        CollaborationLog savedLog = collaborationLogService.addLog(agencyId, log);
        return collaborationLogMapper.toResponseDto(savedLog);
    }

    // GET ALL MESSAGES OF AN AGENCY
    @GetMapping("/agency/{agencyId}")
    public List<CollaborationLogResponseDto> getLogsByAgency(@PathVariable Long agencyId) {
        return collaborationLogService.getLogsByAgency(agencyId)
                .stream()
                .map(collaborationLogMapper::toResponseDto)
                .toList();
    }

    // GET MESSAGES BY USER
    @GetMapping("/user/{userId}")
    public List<CollaborationLogResponseDto> getLogsByUser(@PathVariable Long userId) {
        return collaborationLogService.getLogsByUser(userId)
                .stream()
                .map(collaborationLogMapper::toResponseDto)
                .toList();
    }

    // DELETE MESSAGE
    @DeleteMapping("/{logId}")
    public void deleteLog(@PathVariable Long logId) {
        collaborationLogService.deleteLog(logId);
    }
}