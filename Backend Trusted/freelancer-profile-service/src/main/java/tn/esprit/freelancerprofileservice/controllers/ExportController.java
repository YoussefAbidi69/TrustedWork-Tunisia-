package tn.esprit.freelancerprofileservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.services.ExcelExportService;
import tn.esprit.freelancerprofileservice.services.PdfExportService;

import java.io.IOException;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    @GetMapping("/profiles/{userId}/cv")
    public ResponseEntity<byte[]> exportCv(@PathVariable Long userId) throws IOException {
        byte[] pdf = pdfExportService.generateCv(userId);
        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=cv-freelancer-" + userId + ".pdf")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }

    @GetMapping("/admin/profiles/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportProfilesExcel() throws IOException {
        byte[] excel = excelExportService.generateProfilesExcel();
        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=profiles-export.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excel);
    }
    @GetMapping("/admin/report/pdf")
    public ResponseEntity<byte[]> exportAdminPdf() throws IOException {
        byte[] pdf = pdfExportService.generateAdminReport();

        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment; filename=admin-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}