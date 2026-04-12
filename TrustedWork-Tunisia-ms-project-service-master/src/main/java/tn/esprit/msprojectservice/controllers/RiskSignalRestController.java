package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.DeliveryRiskSignalDTO;
import tn.esprit.msprojectservice.services.IRiskSignalService;
import tn.esprit.msprojectservice.scheduler.DeliveryRiskScheduler;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Signaux de risque IA", description = "Consultation et résolution des alertes de risque générées par l'IA")
public class RiskSignalRestController {

    @Autowired
    private IRiskSignalService riskSignalService;

    @Autowired
    private DeliveryRiskScheduler deliveryRiskScheduler;

    @GetMapping("/projects/{projectId}/risks")
    @Operation(summary = "Risques actifs", description = "Récupérer tous les signaux de risque actifs d'un projet")
    public ResponseEntity<List<DeliveryRiskSignalDTO>> getActiveRisks(@PathVariable Long projectId) {
        List<DeliveryRiskSignalDTO> risks = riskSignalService.getActiveRisksByProjectId(projectId);
        return ResponseEntity.ok(risks);
    }

    @GetMapping("/projects/{projectId}/risks/latest")
    @Operation(summary = "Risque le plus critique", description = "Récupérer le dernier signal de risque le plus critique")
    public ResponseEntity<DeliveryRiskSignalDTO> getLatestCriticalRisk(@PathVariable Long projectId) {
        DeliveryRiskSignalDTO risk = riskSignalService.getLatestCriticalRisk(projectId);
        return ResponseEntity.ok(risk);
    }

    @PatchMapping("/risks/{id}/resolve")
    @Operation(summary = "Résoudre un risque", description = "Marquer un signal de risque comme résolu")
    public ResponseEntity<DeliveryRiskSignalDTO> resolveRisk(@PathVariable Long id) {
        DeliveryRiskSignalDTO resolved = riskSignalService.resolveRisk(id);
        return ResponseEntity.ok(resolved);
    }

    @PostMapping("/projects/{projectId}/risks/analyze")
    @Operation(summary = "Lancer l'analyse IA", description = "Déclencher l'analyse de risque IA manuellement (pour tests et démo)")
    public ResponseEntity<List<DeliveryRiskSignalDTO>> triggerAnalysis(@PathVariable Long projectId) {
        deliveryRiskScheduler.analyzeProjectById(projectId);
        List<DeliveryRiskSignalDTO> risks = riskSignalService.getActiveRisksByProjectId(projectId);
        return ResponseEntity.ok(risks);
    }
}