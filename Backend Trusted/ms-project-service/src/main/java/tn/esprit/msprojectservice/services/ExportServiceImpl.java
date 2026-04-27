package tn.esprit.msprojectservice.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.exceptions.EntityNotFoundException;
import tn.esprit.msprojectservice.exceptions.PdfGenerationException;
import tn.esprit.msprojectservice.repositories.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor  // ✅ Fix @Autowired
public class ExportServiceImpl implements IExportService {

    private final IProjectRepository     projectRepository;   // ✅ final, pas @Autowired
    private final ITaskRepository        taskRepository;
    private final IDeliverableRepository deliverableRepository;
    private final IRiskSignalRepository  riskSignalRepository;

    private static final DateTimeFormatter DATE_FORMAT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ============================================================
    // EXPORT PDF — Rapport complet du projet
    // ✅ Fix Cognitive Complexity — méthode principale allégée,
    //    chaque section déléguée à une méthode privée dédiée
    // ============================================================
    @Override
    public byte[] exportProjectReportPdf(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé avec l'id : " + projectId));

        List<Task>               tasks       = taskRepository.findByProjectId(projectId);
        List<Deliverable>        deliverables = deliverableRepository.findByProjectId(projectId);
        List<DeliveryRiskSignal> risks        = riskSignalRepository.findByProjectIdAndResolvedFalse(projectId);

        // ✅ Fix try-with-resources — PdfWriter et PdfDocument fermés automatiquement
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer  = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document  = new Document(pdfDoc);

            addPdfHeader(document);
            addProjectInfo(document, project);
            addTasksSection(document, tasks);
            addDeliverablesSection(document, deliverables);
            addRisksSection(document, risks);
            addPdfFooter(document);

            document.close();
            return baos.toByteArray();

        } catch (IOException e) {
            // ✅ Fix RuntimeException — exception métier spécifique
            throw new PdfGenerationException("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    // ============================================================
    // Sections PDF — chaque méthode = 1 responsabilité
    // ============================================================

    private void addPdfHeader(Document document) {
        document.add(new Paragraph("RAPPORT DE PROJET")
                .setFontSize(24).setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY));

        document.add(new Paragraph("TrustedWork Tunisia")
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        document.add(new Paragraph("\n"));
    }

    private void addProjectInfo(Document document, Project project) {
        document.add(new Paragraph("1. Informations Générales").setFontSize(16).setBold());
        document.add(new Paragraph("Projet : "            + project.getTitle()));
        document.add(new Paragraph("Description : "       + project.getDescription()));
        document.add(new Paragraph("Statut : "            + project.getStatus()));
        document.add(new Paragraph("Budget : "            + project.getBudget() + " DT"));
        document.add(new Paragraph("Début : "             + formatDate(project.getStartDate())));
        document.add(new Paragraph("Fin prévue : "        + formatDate(project.getEndDate())));
        document.add(new Paragraph("Taux de complétion : " + project.getCompletionRate() + "%"));
        document.add(new Paragraph("\n"));
    }


    private void addTasksSection(Document document, List<Task> tasks) {
        document.add(new Paragraph("2. Liste des Tâches (" + tasks.size() + ")").setFontSize(16).setBold());

        if (tasks.isEmpty()) {
            document.add(new Paragraph("Aucune tâche dans ce projet.").setItalic());
            document.add(new Paragraph("\n"));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2})).useAllAvailableWidth();
        table.addHeaderCell(createHeaderCell("Titre"));
        table.addHeaderCell(createHeaderCell("Statut"));
        table.addHeaderCell(createHeaderCell("Priorité"));
        table.addHeaderCell(createHeaderCell("Deadline"));
        table.addHeaderCell(createHeaderCell("Heures Est."));

        for (Task task : tasks) {
            table.addCell(new Cell().add(new Paragraph(task.getTitle())));
            table.addCell(new Cell().add(new Paragraph(task.getStatus().name())));
            table.addCell(new Cell().add(new Paragraph(task.getPriority().name())));
            table.addCell(new Cell().add(new Paragraph(formatDate(task.getDeadline()))));
            table.addCell(new Cell().add(new Paragraph(
                    task.getEstimatedHours() != null ? task.getEstimatedHours() + "h" : "N/A")));
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addDeliverablesSection(Document document, List<Deliverable> deliverables) {
        document.add(new Paragraph("3. Livrables (" + deliverables.size() + ")").setFontSize(16).setBold());

        if (deliverables.isEmpty()) {
            document.add(new Paragraph("Aucun livrable soumis.").setItalic());
            document.add(new Paragraph("\n"));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 3, 2})).useAllAvailableWidth();
        table.addHeaderCell(createHeaderCell("Titre"));
        table.addHeaderCell(createHeaderCell("Statut"));
        table.addHeaderCell(createHeaderCell("Commentaire"));
        table.addHeaderCell(createHeaderCell("Soumis le"));

        for (Deliverable d : deliverables) {
            table.addCell(new Cell().add(new Paragraph(d.getTitle())));
            table.addCell(new Cell().add(new Paragraph(d.getStatus().name())));
            table.addCell(new Cell().add(new Paragraph(
                    d.getReviewComment() != null ? d.getReviewComment() : "—")));
            table.addCell(new Cell().add(new Paragraph(
                    d.getSubmittedAt() != null ? d.getSubmittedAt().format(DATETIME_FORMAT) : "N/A")));
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addRisksSection(Document document, List<DeliveryRiskSignal> risks) {
        document.add(new Paragraph("4. Signaux de Risque Actifs (" + risks.size() + ")").setFontSize(16).setBold());

        if (risks.isEmpty()) {
            document.add(new Paragraph("Aucun risque actif détecté.").setItalic());
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 2, 4, 2})).useAllAvailableWidth();
        table.addHeaderCell(createHeaderCell("Type"));
        table.addHeaderCell(createHeaderCell("Sévérité"));
        table.addHeaderCell(createHeaderCell("Message"));
        table.addHeaderCell(createHeaderCell("Détecté le"));

        for (DeliveryRiskSignal risk : risks) {
            table.addCell(new Cell().add(new Paragraph(risk.getRiskType().name())));
            table.addCell(new Cell().add(new Paragraph(risk.getSeverity().name())));
            table.addCell(new Cell().add(new Paragraph(risk.getMessage())));
            table.addCell(new Cell().add(new Paragraph(
                    risk.getDetectedAt() != null ? risk.getDetectedAt().format(DATETIME_FORMAT) : "N/A")));
        }

        document.add(table);
    }

    private void addPdfFooter(Document document) {
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Rapport généré automatiquement par TrustedWork Tunisia —")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));
    }

    // ============================================================
    // EXPORT CSV
    // ============================================================
    @Override
    public byte[] exportProjectTasksCsv(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        StringBuilder csv = new StringBuilder();

        csv.append("ID;Titre;Statut;Priorité;Assigné à;Deadline;Heures Estimées;Heures Réelles;Créé le\n");

        for (Task task : tasks) {
            csv.append(task.getId()).append(";");
            csv.append(escapeCSV(task.getTitle())).append(";");
            csv.append(task.getStatus()).append(";");
            csv.append(task.getPriority()).append(";");
            csv.append(task.getAssigneeId() != null ? task.getAssigneeId() : "Non assigné").append(";");
            csv.append(task.getDeadline() != null ? task.getDeadline().format(DATE_FORMAT) : "N/A").append(";");
            csv.append(task.getEstimatedHours() != null ? task.getEstimatedHours() : "N/A").append(";");
            csv.append(task.getActualHours() != null ? task.getActualHours() : "N/A").append(";");
            csv.append(task.getCreatedAt() != null ? task.getCreatedAt().format(DATETIME_FORMAT) : "N/A").append("\n");
        }

        return csv.toString().getBytes();
    }

    // ============================================================
    // Utilitaires
    // ============================================================

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "N/A";
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}