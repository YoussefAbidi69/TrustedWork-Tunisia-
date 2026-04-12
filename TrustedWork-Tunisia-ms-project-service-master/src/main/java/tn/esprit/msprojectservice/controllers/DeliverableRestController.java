package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.DeliverableDTO;
import tn.esprit.msprojectservice.entities.DeliverableStatus;
import tn.esprit.msprojectservice.services.IDeliverableService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Livrables", description = "Gestion des livrables et workflow d'approbation client")
public class DeliverableRestController {

    @Autowired
    private IDeliverableService deliverableService;

    @PostMapping("/projects/{projectId}/deliverables")
    @Operation(summary = "Soumettre un livrable", description = "Le freelancer soumet un livrable pour validation client")
    public ResponseEntity<DeliverableDTO> submitDeliverable(@PathVariable Long projectId, @RequestBody DeliverableDTO deliverableDTO) {
        DeliverableDTO created = deliverableService.submitDeliverable(projectId, deliverableDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/projects/{projectId}/deliverables")
    @Operation(summary = "Lister les livrables", description = "Récupérer tous les livrables d'un projet")
    public ResponseEntity<List<DeliverableDTO>> getDeliverablesByProjectId(@PathVariable Long projectId) {
        List<DeliverableDTO> deliverables = deliverableService.getDeliverablesByProjectId(projectId);
        return ResponseEntity.ok(deliverables);
    }

    @GetMapping("/deliverables/{id}")
    @Operation(summary = "Détails d'un livrable", description = "Récupérer un livrable par son identifiant")
    public ResponseEntity<DeliverableDTO> getDeliverableById(@PathVariable Long id) {
        DeliverableDTO deliverable = deliverableService.getDeliverableById(id);
        return ResponseEntity.ok(deliverable);
    }

    @PatchMapping("/deliverables/{id}/review")
    @Operation(summary = "Approuver ou rejeter", description = "Le client review un livrable : APPROVED ou REJECTED avec commentaire")
    public ResponseEntity<DeliverableDTO> reviewDeliverable(
            @PathVariable Long id,
            @RequestParam DeliverableStatus status,
            @RequestParam(required = false) String reviewComment) {
        DeliverableDTO reviewed = deliverableService.reviewDeliverable(id, status, reviewComment);
        return ResponseEntity.ok(reviewed);
    }

    @DeleteMapping("/deliverables/{id}")
    @Operation(summary = "Supprimer un livrable", description = "Supprimer un livrable")
    public ResponseEntity<Void> deleteDeliverable(@PathVariable Long id) {
        deliverableService.deleteDeliverable(id);
        return ResponseEntity.noContent().build();
    }
}