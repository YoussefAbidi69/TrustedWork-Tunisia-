package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.TaskDTO;
import tn.esprit.msprojectservice.entities.TaskStatus;
import tn.esprit.msprojectservice.services.ITaskService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Tâches", description = "Gestion des tâches et du Kanban Board")
public class TaskRestController {

    @Autowired
    private ITaskService taskService;

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Créer une tâche", description = "Ajouter une nouvelle tâche dans un projet")
    public ResponseEntity<TaskDTO> createTask(@PathVariable Long projectId, @RequestBody TaskDTO taskDTO) {
        TaskDTO created = taskService.createTask(projectId, taskDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "Lister les tâches", description = "Récupérer toutes les tâches d'un projet (alimentation du Kanban)")
    public ResponseEntity<List<TaskDTO>> getTasksByProjectId(@PathVariable Long projectId) {
        List<TaskDTO> tasks = taskService.getTasksByProjectId(projectId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Détails d'une tâche", description = "Récupérer une tâche par son identifiant")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long id) {
        TaskDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Modifier une tâche", description = "Mettre à jour les informations d'une tâche")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Long id, @RequestBody TaskDTO taskDTO) {
        TaskDTO updated = taskService.updateTask(id, taskDTO);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "Déplacer carte Kanban", description = "Changer le statut d'une tâche (Drag & Drop Angular CDK)")
    public ResponseEntity<TaskDTO> updateTaskStatus(@PathVariable Long id, @RequestParam TaskStatus status) {
        TaskDTO updated = taskService.updateTaskStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/tasks/{id}/assign")
    @Operation(summary = "Assigner une tâche", description = "Assigner une tâche à un utilisateur")
    public ResponseEntity<TaskDTO> assignTask(@PathVariable Long id, @RequestParam Long assigneeId) {
        TaskDTO updated = taskService.assignTask(id, assigneeId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Supprimer une tâche", description = "Supprimer une tâche du projet")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}