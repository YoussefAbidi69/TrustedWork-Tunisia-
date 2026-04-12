package tn.esprit.msprojectservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.msprojectservice.services.IExportService;

@RestController
@RequestMapping("/api/projects/{projectId}/export")
@Tag(name = "Export Rapports", description = "Export des rapports en PDF et CSV")
public class ExportRestController {

    @Autowired
    private IExportService exportService;

    @GetMapping("/pdf")
    @Operation(summary = "Export PDF", description = "Télécharger le rapport complet du projet en PDF")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long projectId) {
        byte[] pdfBytes = exportService.exportProjectReportPdf(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rapport_projet_" + projectId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/csv")
    @Operation(summary = "Export CSV", description = "Télécharger la liste des tâches du projet en CSV")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long projectId) {
        byte[] csvBytes = exportService.exportProjectTasksCsv(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=taches_projet_" + projectId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}