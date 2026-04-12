package tn.esprit.msprojectservice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.dto.SubTaskDTO;
import tn.esprit.msprojectservice.entities.SubTask;
import tn.esprit.msprojectservice.entities.Task;
import tn.esprit.msprojectservice.repositories.ISubTaskRepository;
import tn.esprit.msprojectservice.repositories.ITaskRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubTaskServiceImpl implements ISubTaskService {

    @Autowired
    private ISubTaskRepository subTaskRepository;

    @Autowired
    private ITaskRepository taskRepository;

    @Override
    public SubTaskDTO createSubTask(Long taskId, SubTaskDTO subTaskDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée avec l'id : " + taskId));

        SubTask subTask = SubTaskDTO.toEntity(subTaskDTO);
        subTask.setTask(task);

        SubTask saved = subTaskRepository.save(subTask);
        return SubTaskDTO.fromEntity(saved);
    }

    @Override
    public List<SubTaskDTO> getSubTasksByTaskId(Long taskId) {
        return subTaskRepository.findByTaskId(taskId)
                .stream()
                .map(SubTaskDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SubTaskDTO toggleSubTask(Long id) {
        SubTask subTask = subTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sous-tâche non trouvée avec l'id : " + id));

        // Inverser le booléen done
        subTask.setDone(!subTask.isDone());

        SubTask updated = subTaskRepository.save(subTask);
        return SubTaskDTO.fromEntity(updated);
    }

    @Override
    public void deleteSubTask(Long id) {
        SubTask subTask = subTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sous-tâche non trouvée avec l'id : " + id));
        subTaskRepository.delete(subTask);
    }
}