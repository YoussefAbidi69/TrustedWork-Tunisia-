package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.SubTaskDTO;
import tn.esprit.msprojectservice.services.ISubTaskService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Sous-tâches", description = "Gestion des sous-tâches (décomposition granulaire)")
public class SubTaskRestController {

    @Autowired
    private ISubTaskService subTaskService;

    @PostMapping("/tasks/{taskId}/subtasks")
    @Operation(summary = "Ajouter une sous-tâche", description = "Créer une sous-tâche dans une tâche existante")
    public ResponseEntity<SubTaskDTO> createSubTask(@PathVariable Long taskId, @RequestBody SubTaskDTO subTaskDTO) {
        SubTaskDTO created = subTaskService.createSubTask(taskId, subTaskDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/tasks/{taskId}/subtasks")
    @Operation(summary = "Lister les sous-tâches", description = "Récupérer toutes les sous-tâches d'une tâche")
    public ResponseEntity<List<SubTaskDTO>> getSubTasksByTaskId(@PathVariable Long taskId) {
        List<SubTaskDTO> subTasks = subTaskService.getSubTasksByTaskId(taskId);
        return ResponseEntity.ok(subTasks);
    }

    @PatchMapping("/subtasks/{id}/toggle")
    @Operation(summary = "Cocher / décocher", description = "Inverser l'état done d'une sous-tâche")
    public ResponseEntity<SubTaskDTO> toggleSubTask(@PathVariable Long id) {
        SubTaskDTO toggled = subTaskService.toggleSubTask(id);
        return ResponseEntity.ok(toggled);
    }

    @DeleteMapping("/subtasks/{id}")
    @Operation(summary = "Supprimer une sous-tâche", description = "Supprimer une sous-tâche")
    public ResponseEntity<Void> deleteSubTask(@PathVariable Long id) {
        subTaskService.deleteSubTask(id);
        return ResponseEntity.noContent().build();
    }
}