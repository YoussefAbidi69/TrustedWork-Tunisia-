package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.ProjectDTO;
import tn.esprit.msprojectservice.entities.ProjectStatus;
import tn.esprit.msprojectservice.security.AuthenticatedUser;
import tn.esprit.msprojectservice.services.IProjectService;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projets", description = "Gestion des projets TrustedWork")
public class ProjectRestController {

    @Autowired
    private IProjectService projectService;

    // ==================== CRUD DE BASE ====================

    @PostMapping
    @Operation(summary = "Créer un projet", description = "Créer un nouveau projet (déclenché après signature contrat Module 05)")
    public ResponseEntity<ProjectDTO> createProject(@RequestBody ProjectDTO projectDTO) {
        return new ResponseEntity<>(projectService.createProject(projectDTO), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un projet", description = "Récupérer un projet par son identifiant")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping("/contract/{contractId}")
    @Operation(summary = "Projet par contrat", description = "Récupérer le projet lié à un contrat spécifique")
    public ResponseEntity<ProjectDTO> getProjectByContractId(@PathVariable Long contractId) {
        return ResponseEntity.ok(projectService.getProjectByContractId(contractId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Projets d'un utilisateur", description = "Récupérer tous les projets d'un utilisateur (client ou freelancer)")
    public ResponseEntity<List<ProjectDTO>> getProjectsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(projectService.getProjectsByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Tous les projets", description = "Récupérer la liste de tous les projets")
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un projet", description = "Mettre à jour les informations d'un projet existant")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable Long id, @RequestBody ProjectDTO projectDTO) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDTO));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut", description = "Modifier le statut d'un projet (ACTIVE, ON_HOLD, COMPLETED, CANCELLED)")
    public ResponseEntity<ProjectDTO> updateProjectStatus(@PathVariable Long id, @RequestParam ProjectStatus status) {
        return ResponseEntity.ok(projectService.updateProjectStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un projet", description = "Supprimer / archiver un projet")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== ENDPOINTS AVEC JWT ====================

    /**
     * Retourne les projets de l'utilisateur actuellement connecté.
     * Le userId est extrait automatiquement du JWT — pas besoin de le passer en paramètre.
     * Utilisé par le frontend Angular après login.
     */
    @GetMapping("/my")
    @Operation(
            summary = "Mes projets (JWT)",
            description = "Retourne tous les projets du freelancer ou client connecté, " +
                    "enrichis avec CIN, nom, prénom (récupérés depuis le User Service). " +
                    "Requiert un JWT valide dans le header Authorization."
    )
    public ResponseEntity<List<ProjectDTO>> getMyProjects(Authentication authentication) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        Long userId = currentUser.getUserId();
        List<ProjectDTO> projects = projectService.getMyProjects(userId);
        return ResponseEntity.ok(projects);
    }

    /**
     * Retourne un projet enrichi avec les données complètes
     * du client et du freelancer (CIN, nom, prénom, email).
     */
    @GetMapping("/{id}/enriched")
    @Operation(
            summary = "Détail enrichi d'un projet",
            description = "Retourne le projet avec CIN, nom, prénom, email du client et freelancer"
    )
    public ResponseEntity<ProjectDTO> getProjectByIdEnriched(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectByIdEnriched(id));
    }
}