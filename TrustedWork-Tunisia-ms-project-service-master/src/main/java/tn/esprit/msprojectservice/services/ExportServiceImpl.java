package tn.esprit.msprojectservice.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.msprojectservice.entities.*;
import tn.esprit.msprojectservice.repositories.*;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportServiceImpl implements IExportService {

    @Autowired
    private IProjectRepository projectRepository;

    @Autowired
    private ITaskRepository taskRepository;

    @Autowired
    private IDeliverableRepository deliverableRepository;

    @Autowired
    private IRiskSignalRepository riskSignalRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ============================================================
    // EXPORT PDF — Rapport complet du projet
    // ============================================================
    @Override
    public byte[] exportProjectReportPdf(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        List<Task> tasks = taskRepository.findByProjectId(projectId);
        List<Deliverable> deliverables = deliverableRepository.findByProjectId(projectId);
        List<DeliveryRiskSignal> risks = riskSignalRepository.findByProjectIdAndResolvedFalse(projectId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // --- TITRE ---
            document.add(new Paragraph("RAPPORT DE PROJET")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY));

            document.add(new Paragraph("TrustedWork Tunisia — Module 08")
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

            document.add(new Paragraph("\n"));

            // --- INFORMATIONS GÉNÉRALES ---
            document.add(new Paragraph("1. Informations Générales")
                    .setFontSize(16).setBold());

            document.add(new Paragraph("Projet : " + project.getTitle()));
            document.add(new Paragraph("Description : " + project.getDescription()));
            document.add(new Paragraph("Statut : " + project.getStatus()));
            document.add(new Paragraph("Budget : " + project.getBudget() + " DT"));
            document.add(new Paragraph("Début : " + (project.getStartDate() != null ? project.getStartDate().format(DATE_FORMAT) : "N/A")));
            document.add(new Paragraph("Fin prévue : " + (project.getEndDate() != null ? project.getEndDate().format(DATE_FORMAT) : "N/A")));
            document.add(new Paragraph("Taux de complétion : " + project.getCompletionRate() + "%"));

            document.add(new Paragraph("\n"));

            // --- TABLEAU DES TÂCHES ---
            document.add(new Paragraph("2. Liste des Tâches (" + tasks.size() + ")")
                    .setFontSize(16).setBold());

            if (!tasks.isEmpty()) {
                Table taskTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}))
                        .useAllAvailableWidth();

                // En-têtes
                taskTable.addHeaderCell(createHeaderCell("Titre"));
                taskTable.addHeaderCell(createHeaderCell("Statut"));
                taskTable.addHeaderCell(createHeaderCell("Priorité"));
                taskTable.addHeaderCell(createHeaderCell("Deadline"));
                taskTable.addHeaderCell(createHeaderCell("Heures Est."));

                for (Task task : tasks) {
                    taskTable.addCell(new Cell().add(new Paragraph(task.getTitle())));
                    taskTable.addCell(new Cell().add(new Paragraph(task.getStatus().name())));
                    taskTable.addCell(new Cell().add(new Paragraph(task.getPriority().name())));
                    taskTable.addCell(new Cell().add(new Paragraph(
                            task.getDeadline() != null ? task.getDeadline().format(DATE_FORMAT) : "N/A")));
                    taskTable.addCell(new Cell().add(new Paragraph(
                            task.getEstimatedHours() != null ? task.getEstimatedHours() + "h" : "N/A")));
                }

                document.add(taskTable);
            } else {
                document.add(new Paragraph("Aucune tâche dans ce projet.").setItalic());
            }

            document.add(new Paragraph("\n"));

            // --- TABLEAU DES LIVRABLES ---
            document.add(new Paragraph("3. Livrables (" + deliverables.size() + ")")
                    .setFontSize(16).setBold());

            if (!deliverables.isEmpty()) {
                Table deliverableTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 3, 2}))
                        .useAllAvailableWidth();

                deliverableTable.addHeaderCell(createHeaderCell("Titre"));
                deliverableTable.addHeaderCell(createHeaderCell("Statut"));
                deliverableTable.addHeaderCell(createHeaderCell("Commentaire"));
                deliverableTable.addHeaderCell(createHeaderCell("Soumis le"));

                for (Deliverable d : deliverables) {
                    deliverableTable.addCell(new Cell().add(new Paragraph(d.getTitle())));
                    deliverableTable.addCell(new Cell().add(new Paragraph(d.getStatus().name())));
                    deliverableTable.addCell(new Cell().add(new Paragraph(
                            d.getReviewComment() != null ? d.getReviewComment() : "—")));
                    deliverableTable.addCell(new Cell().add(new Paragraph(
                            d.getSubmittedAt() != null ? d.getSubmittedAt().format(DATETIME_FORMAT) : "N/A")));
                }

                document.add(deliverableTable);
            } else {
                document.add(new Paragraph("Aucun livrable soumis.").setItalic());
            }

            document.add(new Paragraph("\n"));

            // --- SIGNAUX DE RISQUE ---
            document.add(new Paragraph("4. Signaux de Risque Actifs (" + risks.size() + ")")
                    .setFontSize(16).setBold());

            if (!risks.isEmpty()) {
                Table riskTable = new Table(UnitValue.createPercentArray(new float[]{2, 2, 4, 2}))
                        .useAllAvailableWidth();

                riskTable.addHeaderCell(createHeaderCell("Type"));
                riskTable.addHeaderCell(createHeaderCell("Sévérité"));
                riskTable.addHeaderCell(createHeaderCell("Message"));
                riskTable.addHeaderCell(createHeaderCell("Détecté le"));

                for (DeliveryRiskSignal risk : risks) {
                    riskTable.addCell(new Cell().add(new Paragraph(risk.getRiskType().name())));
                    riskTable.addCell(new Cell().add(new Paragraph(risk.getSeverity().name())));
                    riskTable.addCell(new Cell().add(new Paragraph(risk.getMessage())));
                    riskTable.addCell(new Cell().add(new Paragraph(
                            risk.getDetectedAt() != null ? risk.getDetectedAt().format(DATETIME_FORMAT) : "N/A")));
                }

                document.add(riskTable);
            } else {
                document.add(new Paragraph("Aucun risque actif détecté. ✅").setItalic());
            }

            // --- FOOTER ---
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Rapport généré automatiquement par TrustedWork Tunisia — Module 08")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF : " + e.getMessage());
        }
    }

    // ============================================================
    // EXPORT CSV — Liste des tâches
    // ============================================================
    @Override
    public byte[] exportProjectTasksCsv(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'id : " + projectId));

        List<Task> tasks = taskRepository.findByProjectId(projectId);

        StringBuilder csv = new StringBuilder();

        // En-tête CSV
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

    // --- Utilitaires ---
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