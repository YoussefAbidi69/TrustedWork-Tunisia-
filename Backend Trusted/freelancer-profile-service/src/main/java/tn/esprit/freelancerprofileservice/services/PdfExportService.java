package tn.esprit.freelancerprofileservice.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.freelancerprofileservice.clients.UserClient;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.enums.ReportStatus;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;
import tn.esprit.freelancerprofileservice.exceptions.ResourceNotFoundException;
import tn.esprit.freelancerprofileservice.repositories.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final FreelancerProfileRepository profileRepository;
    private final SkillRepository skillRepository;
    private final PortfolioItemRepository portfolioRepository;
    private final CertificationRepository certificationRepository;
    private final ProfileReportRepository reportRepository;
    private final ProfileReviewRepository reviewRepository;
    private final UserClient userClient;

    // Couleurs branding TrustedWork
    private static final DeviceRgb COLOR_PRIMARY   = new DeviceRgb(30, 64, 175);   // bleu foncé
    private static final DeviceRgb COLOR_SECONDARY = new DeviceRgb(99, 102, 241);  // indigo
    private static final DeviceRgb COLOR_SUCCESS   = new DeviceRgb(22, 163, 74);   // vert
    private static final DeviceRgb COLOR_WARNING   = new DeviceRgb(234, 88, 12);   // orange
    private static final DeviceRgb COLOR_LIGHT_BG  = new DeviceRgb(248, 250, 252); // gris clair

    // =========================
    //  FRONT (CV FREELANCER)
    // =========================
    public byte[] generateCv(Long userId) throws IOException {
        FreelancerProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil", userId));

        String fullName = userClient.getUserFullName(userId);
        var skills = skillRepository.findByProfileIdOrderByAuthenticityScoreDesc(profile.getId());
        var portfolio = portfolioRepository.findByProfileIdOrderByPinnedDescCompletionDateDescIdDesc(profile.getId());
        var certifications = certificationRepository.findByProfileIdOrderByIssueDateDesc(profile.getId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // En-tête CV
        document.add(new Paragraph("CURRICULUM VITAE")
                .setFontSize(20).setBold()
                .setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(fullName != null ? fullName : "Freelancer")
                .setFontSize(16).setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(profile.getHeadline() != null ? profile.getHeadline() : "")
                .setFontSize(12).setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        document.add(new LineSeparator(new SolidLine()));

        document.add(new Paragraph("\nRégion : " + safe(profile.getRegion())));
        document.add(new Paragraph("Taux horaire : " + safe(profile.getHourlyRate()) + " TND/h"));
        document.add(new Paragraph("Score de complétude : " + safe(profile.getCompletenessScore()) + "%"));
        document.add(new Paragraph("Total vues : " + safe(profile.getTotalViews())));

        if (profile.getBio() != null && !profile.getBio().isBlank()) {
            document.add(new Paragraph("\nÀ propos").setBold().setFontColor(COLOR_PRIMARY));
            document.add(new Paragraph(profile.getBio()));
        }

        document.add(new Paragraph("\nCompétences").setBold().setFontColor(COLOR_PRIMARY));
        skills.forEach(s -> document.add(new Paragraph(
                "• " + s.getName() + " (" + safe(s.getLevel()) + ")")));

        if (!certifications.isEmpty()) {
            document.add(new Paragraph("\nCertifications").setBold().setFontColor(COLOR_PRIMARY));
            certifications.forEach(c -> document.add(new Paragraph("• " + c.getTitle())));
        }

        if (!portfolio.isEmpty()) {
            document.add(new Paragraph("\nPortfolio").setBold().setFontColor(COLOR_PRIMARY));
            portfolio.forEach(p -> document.add(new Paragraph("• " + p.getTitle())));
        }

        document.add(new Paragraph("\n— Généré automatiquement par TrustedWork Tunisia —")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(9)
                .setFontColor(ColorConstants.GRAY));

        document.close();
        return baos.toByteArray();
    }

    // =========================
    // ADMIN PDF REPORT
    // =========================
    public byte[] generateAdminReport() throws IOException {

        List<FreelancerProfile> profiles = profileRepository.findAll();

        // Calcul des statistiques
        long totalProfiles    = profiles.size();
        long suspended        = profiles.stream().filter(p -> Boolean.TRUE.equals(p.getSuspended())).count();
        long available        = profiles.stream().filter(p -> AvailabilityStatus.AVAILABLE.equals(p.getAvailabilityStatus())).count();
        long highQuality      = profiles.stream().filter(p -> p.getCompletenessScore() != null && p.getCompletenessScore() >= 80).count();
        long mediumQuality    = profiles.stream().filter(p -> p.getCompletenessScore() != null && p.getCompletenessScore() >= 50 && p.getCompletenessScore() < 80).count();
        long lowQuality       = profiles.stream().filter(p -> p.getCompletenessScore() != null && p.getCompletenessScore() < 50).count();
        double avgCompleteness = profiles.stream().filter(p -> p.getCompletenessScore() != null).mapToInt(FreelancerProfile::getCompletenessScore).average().orElse(0);
        long totalViews       = profiles.stream().filter(p -> p.getTotalViews() != null).mapToLong(FreelancerProfile::getTotalViews).sum();

        long totalReports     = reportRepository.count();
        long pendingReports   = reportRepository.findByStatus(ReportStatus.PENDING).size();
        long resolvedReports  = reportRepository.findByStatus(ReportStatus.RESOLVED).size();

        long totalSkills      = skillRepository.count();

        // Stats par région
        Map<String, Long> byRegion = profiles.stream()
                .filter(p -> p.getRegion() != null)
                .collect(Collectors.groupingBy(FreelancerProfile::getRegion, Collectors.counting()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdfDoc);

        // ── EN-TÊTE ──────────────────────────────────────────────
        document.add(new Paragraph("TrustedWork Tunisia")
                .setFontSize(22).setBold()
                .setFontColor(COLOR_PRIMARY)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("RAPPORT ADMINISTRATEUR — MODULE 02")
                .setFontSize(13).setFontColor(COLOR_SECONDARY)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Généré le : " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFontSize(10).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("\n"));

        // ── SECTION 1 : VUE D'ENSEMBLE ───────────────────────────
        document.add(sectionTitle("1. Vue d'ensemble de la plateforme"));

        Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

        addStatRow(overviewTable, "Total profils freelancers", String.valueOf(totalProfiles));
        addStatRow(overviewTable, "Profils disponibles", available + " (" + pct(available, totalProfiles) + "%)");
        addStatRow(overviewTable, "Profils suspendus", suspended + " (" + pct(suspended, totalProfiles) + "%)");
        addStatRow(overviewTable, "Total vues cumulées", String.valueOf(totalViews));
        addStatRow(overviewTable, "Total compétences déclarées", String.valueOf(totalSkills));
        addStatRow(overviewTable, "Score de complétude moyen", String.format("%.1f%%", avgCompleteness));

        document.add(overviewTable);
        document.add(new Paragraph("\n"));

        // ── SECTION 2 : QUALITÉ DES PROFILS ──────────────────────
        document.add(sectionTitle("2. Qualité des profils"));

        Table qualityTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30})).useAllAvailableWidth();

        // En-tête
        qualityTable.addHeaderCell(headerCell("Catégorie"));
        qualityTable.addHeaderCell(headerCell("Nombre"));
        qualityTable.addHeaderCell(headerCell("Pourcentage"));

        qualityTable.addCell(new Cell().add(new Paragraph("🟢 Haute qualité (≥ 80%)")));
        qualityTable.addCell(new Cell().add(new Paragraph(String.valueOf(highQuality))));
        qualityTable.addCell(new Cell().add(new Paragraph(pct(highQuality, totalProfiles) + "%")));

        qualityTable.addCell(new Cell().add(new Paragraph("🟡 Qualité moyenne (50-79%)")));
        qualityTable.addCell(new Cell().add(new Paragraph(String.valueOf(mediumQuality))));
        qualityTable.addCell(new Cell().add(new Paragraph(pct(mediumQuality, totalProfiles) + "%")));

        qualityTable.addCell(new Cell().add(new Paragraph("🔴 Faible qualité (< 50%)")));
        qualityTable.addCell(new Cell().add(new Paragraph(String.valueOf(lowQuality))));
        qualityTable.addCell(new Cell().add(new Paragraph(pct(lowQuality, totalProfiles) + "%")));

        document.add(qualityTable);
        document.add(new Paragraph("\n"));

        // ── SECTION 3 : SIGNALEMENTS ─────────────────────────────
        document.add(sectionTitle("3. Signalements (Reports)"));

        Table reportTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        addStatRow(reportTable, "Total signalements", String.valueOf(totalReports));
        addStatRow(reportTable, "En attente de traitement", String.valueOf(pendingReports));
        addStatRow(reportTable, "Résolus", String.valueOf(resolvedReports));
        addStatRow(reportTable, "Taux de résolution",
                totalReports > 0 ? pct(resolvedReports, totalReports) + "%" : "N/A");
        document.add(reportTable);
        document.add(new Paragraph("\n"));

        // ── SECTION 4 : RÉPARTITION PAR RÉGION ───────────────────
        if (!byRegion.isEmpty()) {
            document.add(sectionTitle("4. Répartition par région"));

            Table regionTable = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25})).useAllAvailableWidth();
            regionTable.addHeaderCell(headerCell("Région"));
            regionTable.addHeaderCell(headerCell("Profils"));
            regionTable.addHeaderCell(headerCell("%"));

            byRegion.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> {
                        regionTable.addCell(new Cell().add(new Paragraph(entry.getKey())));
                        regionTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue()))));
                        regionTable.addCell(new Cell().add(new Paragraph(pct(entry.getValue(), totalProfiles) + "%")));
                    });

            document.add(regionTable);
            document.add(new Paragraph("\n"));
        }

        // ── TOP 5 PROFILS LES PLUS CONSULTÉS ─────────────────────
        document.add(sectionTitle("5. Top 5 profils les plus consultés"));

        Table topTable = new Table(UnitValue.createPercentArray(new float[]{10, 50, 20, 20})).useAllAvailableWidth();
        topTable.addHeaderCell(headerCell("#"));
        topTable.addHeaderCell(headerCell("Headline"));
        topTable.addHeaderCell(headerCell("Région"));
        topTable.addHeaderCell(headerCell("Vues"));

        profiles.stream()
                .filter(p -> p.getTotalViews() != null)
                .sorted((a, b) -> Integer.compare(b.getTotalViews(), a.getTotalViews()))
                .limit(5)
                .forEach(p -> {
                    int rank = profiles.indexOf(p) + 1;
                    topTable.addCell(new Cell().add(new Paragraph(String.valueOf(rank))));
                    topTable.addCell(new Cell().add(new Paragraph(safe(p.getHeadline()))));
                    topTable.addCell(new Cell().add(new Paragraph(safe(p.getRegion()))));
                    topTable.addCell(new Cell().add(new Paragraph(safe(p.getTotalViews()))));
                });

        document.add(topTable);

        // ── PIED DE PAGE ──────────────────────────────────────────
        document.add(new Paragraph("\n\n"));
        document.add(new LineSeparator(new SolidLine()));
        document.add(new Paragraph("Rapport généré automatiquement par TrustedWork Tunisia — Module 02 : Freelancer Profile Service")
                .setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("ESPRIT School of Engineering — Projet PI Cloud S8 — " + LocalDate.now().getYear())
                .setFontSize(8).setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
        return baos.toByteArray();
    }

    // =========================
    // HELPERS
    // =========================

    private Paragraph sectionTitle(String title) {
        return new Paragraph(title)
                .setFontSize(13).setBold()
                .setFontColor(COLOR_PRIMARY)
                .setMarginTop(8).setMarginBottom(4);
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(COLOR_PRIMARY);
    }

    private void addStatRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold()));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    private String pct(long part, long total) {
        if (total == 0) return "0";
        return String.format("%.1f", (part * 100.0) / total);
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "—";
    }
}