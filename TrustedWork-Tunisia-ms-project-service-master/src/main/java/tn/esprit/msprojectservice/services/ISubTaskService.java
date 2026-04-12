package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.SubTaskDTO;

import java.util.List;

public interface ISubTaskService {

    SubTaskDTO createSubTask(Long taskId, SubTaskDTO subTaskDTO);

    List<SubTaskDTO> getSubTasksByTaskId(Long taskId);

    SubTaskDTO toggleSubTask(Long id);

    void deleteSubTask(Long id);
}