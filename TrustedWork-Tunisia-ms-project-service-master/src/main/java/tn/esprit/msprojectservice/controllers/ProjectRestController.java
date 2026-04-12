package tn.esprit.msprojectservice.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.entities.ProjectStatus;
import tn.esprit.msprojectservice.services.IProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projets", description = "Gestion des projets TrustedWork")
public class ProjectRestController {

    @Autowired
    private IProjectService projectService;

    @PostMapping
    @Operation(summary = "Créer un projet", description = "Créer un nouveau projet (déclenché après signature contrat Module 05)")
    public ResponseEntity<ProjectDTO> createProject(@RequestBody ProjectDTO projectDTO) {
        ProjectDTO created = projectService.createProject(projectDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un projet", description = "Récupérer un projet par son identifiant")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Projet par contrat", description = "Récupérer le projet lié à un contrat spécifique")
    public ResponseEntity<ProjectDTO> getProjectByContractId(@PathVariable Long contractId) {
        ProjectDTO project = projectService.getProjectByContractId(contractId);
        return ResponseEntity.ok(project);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Projets d'un utilisateur", description = "Récupérer tous les projets d'un utilisateur (client ou freelancer)")
    public ResponseEntity<List<ProjectDTO>> getProjectsByUserId(@PathVariable Long userId) {
        List<ProjectDTO> projects = projectService.getProjectsByUserId(userId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping
    @Operation(summary = "Tous les projets", description = "Récupérer la liste de tous les projets")
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un projet", description = "Mettre à jour les informations d'un projet existant")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable Long id, @RequestBody ProjectDTO projectDTO) {
        ProjectDTO updated = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut", description = "Modifier le statut d'un projet (ACTIVE, ON_HOLD, COMPLETED, CANCELLED)")
    public ResponseEntity<ProjectDTO> updateProjectStatus(@PathVariable Long id, @RequestParam ProjectStatus status) {
        ProjectDTO updated = projectService.updateProjectStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un projet", description = "Supprimer / archiver un projet")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}