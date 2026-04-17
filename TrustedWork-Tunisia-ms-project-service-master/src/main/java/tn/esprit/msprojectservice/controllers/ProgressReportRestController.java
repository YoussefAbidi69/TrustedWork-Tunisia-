package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.dto.ProgressReportDTO;
import tn.esprit.msprojectservice.services.IProgressReportService;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
@Tag(name = "Rapports de progression", description = "Génération et consultation des rapports de progression automatiques")
public class ProgressReportRestController {

    private final IProgressReportService progressReportService;

    @GetMapping("/report")
    @Operation(summary = "Générer un rapport", description = "Générer le rapport de progression actuel du projet")
    public ResponseEntity<ProgressReportDTO> generateReport(@PathVariable Long projectId) {
        return ResponseEntity.ok(progressReportService.generateReport(projectId));
    }

    @GetMapping("/reports")
    @Operation(summary = "Historique des rapports", description = "Récupérer tous les rapports de progression d'un projet")
    public ResponseEntity<List<ProgressReportDTO>> getReportHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(progressReportService.getReportHistory(projectId));
    }
}