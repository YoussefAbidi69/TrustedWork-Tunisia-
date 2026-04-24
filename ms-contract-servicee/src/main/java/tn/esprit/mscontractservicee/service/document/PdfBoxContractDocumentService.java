package tn.esprit.mscontractservicee.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.enums.SignatureType;
import tn.esprit.mscontractservicee.feign.UserServiceClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class PdfBoxContractDocumentService implements ContractDocumentService {

    private final UserServiceClient userServiceClient;

    public PdfBoxContractDocumentService(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private static final Color TWT_PRIMARY = new Color(0, 150, 175); // TWT Blue
    private static final Color TWT_SECONDARY = new Color(80, 80, 80);
    private static final Color LIGHT_GRAY = new Color(240, 240, 240);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    @Override
    public byte[] generateContractPdf(Contract contract, List<Milestone> milestones, List<SignatureSigner> signers) {
        if (contract == null) throw new IllegalArgumentException("contract is required");
        List<Milestone> safeMilestones = milestones != null ? milestones : List.of();
        List<SignatureSigner> safeSigners = signers != null ? signers : List.of();

        try (PDDocument doc = new PDDocument()) {
            PDRectangle box = PDRectangle.A4;
            float margin = 50f;
            float width = box.getWidth() - 2 * margin;

            PDPage page = new PDPage(box);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = box.getHeight() - margin;

            try {
                UserDTO clientDto = fetchUserByCin(contract.getClientCin());
                UserDTO freelancerDto = fetchUserByCin(contract.getFreelancerCin());

                // Header
                y = drawHeader(doc, cs, box, margin, y, contract);

                // Info Section
                y = drawContractInfo(cs, margin, width, y, contract);

                // Participants
                y = drawParticipants(cs, margin, width, y, contract, safeSigners, clientDto, freelancerDto);

                // Project Details
                y = drawProjectDetails(cs, margin, width, y, contract);

                // Milestones
                if (!safeMilestones.isEmpty()) {
                    y = drawMilestonesTable(cs, margin, width, y, safeMilestones, doc, box);
                }

                // Total
                y = drawTotalBlock(cs, margin, width, y, contract);

                // Signatures
                addSignaturesPage(doc, contract, safeSigners, clientDto, freelancerDto);

            } finally {
                if (cs != null) {
                    try { cs.close(); } catch (Exception ignore) {}
                }
            }
            return toBytes(doc);
        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private float drawHeader(PDDocument doc, PDPageContentStream cs, PDRectangle box, float margin, float y, Contract contract) throws Exception {
        // Try to load logo
        try {
            ClassPathResource logoResource = new ClassPathResource("logo.png"); // TWT Logo
            if (logoResource.exists()) {
                try (InputStream is = logoResource.getInputStream()) {
                    PDImageXObject logo = PDImageXObject.createFromByteArray(doc, is.readAllBytes(), "logo");
                    float logoWidth = 120f;
                    float scale = logoWidth / logo.getWidth();
                    float logoHeight = logo.getHeight() * scale;
                    cs.drawImage(logo, margin, y - logoHeight + 10, logoWidth, logoHeight);
                }
            } else {
                // Fallback text logo
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 26);
                cs.setNonStrokingColor(TWT_PRIMARY);
                cs.newLineAtOffset(margin, y - 10);
                cs.showText("TWT");
                cs.endText();
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.setNonStrokingColor(TWT_SECONDARY);
                cs.newLineAtOffset(margin, y - 25);
                cs.showText("TrustedWork Tunisia");
                cs.endText();
            }
        } catch (Exception e) {
            log.warn("Could not load logo", e);
        }

        // Title
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
        cs.setNonStrokingColor(TWT_PRIMARY);
        String title = "CONTRAT DE PRESTATION";
        float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * 22;
        cs.newLineAtOffset(box.getWidth() - margin - titleWidth, y - 10);
        cs.showText(title);
        cs.endText();

        // Ref
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.setNonStrokingColor(TWT_SECONDARY);
        String ref = "Réf: " + nvl(contract.getReference());
        float refWidth = PDType1Font.HELVETICA.getStringWidth(ref) / 1000 * 11;
        cs.newLineAtOffset(box.getWidth() - margin - refWidth, y - 30);
        cs.showText(ref);
        cs.endText();

        // Line
        y -= 60;
        cs.setStrokingColor(TWT_PRIMARY);
        cs.setLineWidth(2f);
        cs.moveTo(margin, y);
        cs.lineTo(box.getWidth() - margin, y);
        cs.stroke();

        return y - 30;
    }

    private float drawContractInfo(PDPageContentStream cs, float x, float width, float y, Contract contract) throws Exception {
        cs.setNonStrokingColor(TWT_SECONDARY);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("INFORMATIONS GÉNÉRALES");
        cs.endText();
        
        y -= 25;
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.setNonStrokingColor(Color.BLACK);
        
        String dateCreation = contract.getCreatedAt() != null ? DATE_ONLY.format(contract.getCreatedAt()) : "-";
        String dateFin = contract.getDateFin() != null ? DATE_ONLY.format(contract.getDateFin()) : "-";
        
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText("Statut du contrat : " + (contract.getStatus() != null ? contract.getStatus().name() : "-"));
        cs.newLineAtOffset(0, -15);
        cs.showText("Date de création : " + dateCreation);
        cs.newLineAtOffset(0, -15);
        cs.showText("Date de fin estimée : " + dateFin);
        cs.endText();

        return y - 45;
    }

    private float drawParticipants(PDPageContentStream cs, float margin, float width, float y, Contract contract, List<SignatureSigner> signers, UserDTO clientDto, UserDTO freelancerDto) throws Exception {
        cs.setNonStrokingColor(TWT_SECONDARY);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.beginText();
        cs.newLineAtOffset(margin, y);
        cs.showText("LES PARTIES");
        cs.endText();

        y -= 25;
        
        float midX = margin + (width / 2);
        float boxHeight = 90f;
        
        // Client Box
        drawBoxFill(cs, margin, y - boxHeight + 10, (width / 2) - 10, boxHeight, LIGHT_GRAY);
        cs.setNonStrokingColor(Color.BLACK);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(margin + 10, y - 5);
        cs.showText("LE CLIENT");
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(0, -18);
        if (clientDto != null && clientDto.getFirstName() != null) {
            cs.showText("Nom : " + clientDto.getFirstName() + " " + nvl(clientDto.getLastName()));
            cs.newLineAtOffset(0, -15);
        }
        cs.showText("CIN: " + nvl(contract.getClientCin()));
        SignatureSigner client = findSigner(signers, SignerRole.CLIENT);
        if (client != null && client.getSignerEmail() != null) {
            cs.newLineAtOffset(0, -15);
            cs.showText("Email: " + client.getSignerEmail());
        }
        cs.endText();

        // Freelancer Box
        drawBoxFill(cs, midX + 10, y - boxHeight + 10, (width / 2) - 10, boxHeight, LIGHT_GRAY);
        cs.setNonStrokingColor(Color.BLACK);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(midX + 20, y - 5);
        cs.showText("LE PRESTATAIRE (FREELANCER)");
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(0, -18);
        if (freelancerDto != null && freelancerDto.getFirstName() != null) {
            cs.showText("Nom : " + freelancerDto.getFirstName() + " " + nvl(freelancerDto.getLastName()));
            cs.newLineAtOffset(0, -15);
        }
        cs.showText("CIN: " + nvl(contract.getFreelancerCin()));
        SignatureSigner freelancer = findSigner(signers, SignerRole.FREELANCER);
        if (freelancer != null && freelancer.getSignerEmail() != null) {
            cs.newLineAtOffset(0, -15);
            cs.showText("Email: " + freelancer.getSignerEmail());
        }
        cs.endText();

        return y - 100;
    }

    private float drawProjectDetails(PDPageContentStream cs, float margin, float width, float y, Contract contract) throws Exception {
        cs.setNonStrokingColor(TWT_SECONDARY);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.beginText();
        cs.newLineAtOffset(margin, y);
        cs.showText("DÉTAILS DU PROJET");
        cs.endText();

        y -= 25;
        cs.setNonStrokingColor(Color.BLACK);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText();
        cs.newLineAtOffset(margin, y);
        cs.showText("Titre : " + nvl(contract.getProjectTitle()));
        cs.endText();

        y -= 20;
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.setNonStrokingColor(TWT_SECONDARY);
        List<String> descLines = wrap(nvl(contract.getDescription()), 90);
        for (String line : descLines) {
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText(line);
            cs.endText();
            y -= 14;
        }

        return y - 20;
    }

    private float drawMilestonesTable(PDPageContentStream cs, float margin, float width, float y, List<Milestone> milestones, PDDocument doc, PDRectangle box) throws Exception {
        cs.setNonStrokingColor(TWT_SECONDARY);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.beginText();
        cs.newLineAtOffset(margin, y);
        cs.showText("JALONS ET PAIEMENTS");
        cs.endText();
        y -= 15;

        // Table Header
        drawBoxFill(cs, margin, y - 20, width, 25, TWT_PRIMARY);
        cs.setNonStrokingColor(Color.WHITE);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.beginText();
        cs.newLineAtOffset(margin + 5, y - 12);
        cs.showText("N°");
        cs.newLineAtOffset(30, 0);
        cs.showText("Titre du Jalon");
        cs.newLineAtOffset(210, 0);
        cs.showText("Date limite");
        cs.newLineAtOffset(110, 0);
        cs.showText("Statut");
        cs.newLineAtOffset(80, 0);
        cs.showText("Montant (DT)");
        cs.endText();

        y -= 20;
        cs.setNonStrokingColor(Color.BLACK);
        cs.setFont(PDType1Font.HELVETICA, 9);

        boolean alt = false;
        int idx = 1;
        for (Milestone m : milestones) {
            if (alt) {
                drawBoxFill(cs, margin, y - 20, width, 20, new Color(245, 245, 245));
            }
            cs.beginText();
            cs.newLineAtOffset(margin + 5, y - 12);
            cs.showText(String.valueOf(idx++));
            cs.newLineAtOffset(30, 0);
            cs.showText(safeText(nvl(m.getTitre()), 40));
            cs.newLineAtOffset(210, 0);
            String deadline = m.getDeadline() != null ? DATE_ONLY.format(m.getDeadline()) : "-";
            cs.showText(deadline);
            cs.newLineAtOffset(110, 0);
            cs.showText(m.getStatus() != null ? m.getStatus().name() : "-");
            cs.newLineAtOffset(80, 0);
            cs.showText(String.format(Locale.US, "%.2f", m.getMontant() != null ? m.getMontant() : 0.0));
            cs.endText();

            y -= 20;
            alt = !alt;
        }

        return y - 10;
    }

    private float drawTotalBlock(PDPageContentStream cs, float margin, float width, float y, Contract contract) throws Exception {
        float boxWidth = 220f;
        float x = margin + width - boxWidth;
        
        drawBoxFill(cs, x, y - 40, boxWidth, 35, LIGHT_GRAY);
        cs.setNonStrokingColor(TWT_PRIMARY);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.beginText();
        cs.newLineAtOffset(x + 15, y - 26);
        cs.showText("MONTANT TOTAL :");
        
        cs.setNonStrokingColor(Color.BLACK);
        String total = String.format(Locale.US, "%.2f DT", contract.getMontantTotal() != null ? contract.getMontantTotal() : 0.0);
        float tw = PDType1Font.HELVETICA_BOLD.getStringWidth(total) / 1000 * 12;
        cs.newLineAtOffset(boxWidth - 30 - tw, 0);
        cs.showText(total);
        cs.endText();

        return y - 60;
    }

    private void drawBoxFill(PDPageContentStream cs, float x, float y, float width, float height, Color color) throws Exception {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, width, height);
        cs.fill();
    }

    private static void addSignaturesPage(PDDocument doc, Contract contract, List<SignatureSigner> signers, UserDTO clientDto, UserDTO freelancerDto) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;

            cs.setNonStrokingColor(TWT_PRIMARY);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("SIGNATURES ÉLECTRONIQUES");
            cs.endText();
            
            y -= 15;
            cs.setStrokingColor(TWT_PRIMARY);
            cs.setLineWidth(2f);
            cs.moveTo(margin, y);
            cs.lineTo(page.getMediaBox().getWidth() - margin, y);
            cs.stroke();

            y -= 30;
            cs.setNonStrokingColor(TWT_SECONDARY);
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            cs.beginText();
            cs.newLineAtOffset(margin, y);
            cs.showText("Référence du Contrat : " + nvl(contract.getReference()));
            cs.endText();

            y -= 50;

            SignatureSigner client = findSigner(signers, SignerRole.CLIENT);
            SignatureSigner freelancer = findSigner(signers, SignerRole.FREELANCER);

            float colWidth = (page.getMediaBox().getWidth() - (margin * 3)) / 2;
            float blockHeight = 180f;

            renderSignerBlock(doc, cs, margin, y, "LE CLIENT", client, colWidth, blockHeight, clientDto);
            renderSignerBlock(doc, cs, margin * 2 + colWidth, y, "LE PRESTATAIRE", freelancer, colWidth, blockHeight, freelancerDto);

            // Audit text at the bottom
            cs.setNonStrokingColor(TWT_SECONDARY);
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
            cs.beginText();
            cs.newLineAtOffset(margin, 50);
            cs.showText("Ce document est généré par TrustedWork Tunisia. Les signatures électroniques ont valeur légale conformément à la loi en vigueur.");
            cs.newLineAtOffset(0, -12);
            cs.showText("Horodatage et pistes d'audit conservés de manière sécurisée.");
            cs.endText();
        }
    }

    private static SignatureSigner findSigner(List<SignatureSigner> signers, SignerRole role) {
        if (signers == null || role == null) return null;
        return signers.stream()
                .filter(s -> s != null && s.getRole() == role)
                .max(Comparator.comparing(s -> Optional.ofNullable(s.getSignedAt()).orElse(null),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static void renderSignerBlock(PDDocument doc, PDPageContentStream cs, float x, float topY, String label, SignatureSigner signer, float width, float height, UserDTO dto) throws Exception {
        // Draw Border
        cs.setStrokingColor(BORDER_COLOR);
        cs.setLineWidth(1f);
        cs.addRect(x, topY - height, width, height);
        cs.stroke();
        
        // Header Fill
        cs.setNonStrokingColor(LIGHT_GRAY);
        cs.addRect(x, topY - 30, width, 30);
        cs.fill();

        float y = topY - 20;
        
        cs.setNonStrokingColor(Color.BLACK);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText();
        cs.newLineAtOffset(x + 10, y);
        cs.showText(label);
        cs.endText();
        
        y -= 30;

        if (signer == null) {
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            cs.setNonStrokingColor(TWT_SECONDARY);
            cs.beginText();
            cs.newLineAtOffset(x + 10, y);
            cs.showText("Non assigné");
            cs.endText();
            return;
        }

        if (dto != null && dto.getFirstName() != null) {
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            cs.setNonStrokingColor(TWT_SECONDARY);
            cs.beginText();
            cs.newLineAtOffset(x + 10, y);
            cs.showText("Nom : ");
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.setNonStrokingColor(Color.BLACK);
            cs.showText(dto.getFirstName() + " " + nvl(dto.getLastName()));
            cs.endText();
            y -= 15;
        }

        String email = nvl(signer.getSignerEmail());
        String status = signer.getStatus() != null ? signer.getStatus().name() : "PENDING";
        
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.setNonStrokingColor(TWT_SECONDARY);
        cs.beginText();
        cs.newLineAtOffset(x + 10, y);
        cs.showText("Email : ");
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.setNonStrokingColor(Color.BLACK);
        cs.showText(email);
        cs.endText();
        y -= 15;
        
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.setNonStrokingColor(TWT_SECONDARY);
        cs.beginText();
        cs.newLineAtOffset(x + 10, y);
        cs.showText("Statut : ");
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.setNonStrokingColor(Color.BLACK);
        cs.showText(status);
        cs.endText();
        y -= 15;

        if (signer.getStatus() != SignatureSignerStatus.SIGNED) {
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            cs.setNonStrokingColor(TWT_SECONDARY);
            cs.beginText();
            cs.newLineAtOffset(x + 10, topY - height + 30);
            cs.showText("En attente de signature...");
            cs.endText();
            return;
        }

        if (signer.getSignedAt() != null) {
            cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
            cs.setNonStrokingColor(TWT_SECONDARY);
            cs.beginText();
            cs.newLineAtOffset(x + 10, y);
            cs.showText("Horodatage : ");
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.setNonStrokingColor(Color.BLACK);
            cs.showText(DT.format(signer.getSignedAt()));
            cs.endText();
        }

        y -= 10;

        SignatureType type = signer.getSignatureType();
        String payload = signer.getSignaturePayload();
        
        if (type == SignatureType.DRAWN && payload != null && !payload.isBlank()) {
            byte[] png = tryDecodePng(payload);
            if (png != null) {
                try {
                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, png, "signature");
                    float maxW = width - 20;
                    float maxH = height - 90;
                    float iw = img.getWidth();
                    float ih = img.getHeight();
                    float scale = Math.min(maxW / iw, maxH / ih);
                    float w = iw * scale;
                    float h = ih * scale;
                    cs.drawImage(img, x + 10, topY - height + 10, w, h);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to draw signature image", e);
                }
            }
        }

        // Fallback Typed signature
        cs.setFont(PDType1Font.TIMES_ITALIC, 14);
        cs.setNonStrokingColor(TWT_PRIMARY);
        cs.beginText();
        cs.newLineAtOffset(x + 10, topY - height + 30);
        cs.showText(payload != null ? safeText(payload, 25) : "Signé électroniquement");
        cs.endText();
    }

    private UserDTO fetchUserByCin(Long cin) {
        if (cin == null) return null;
        try {
            return userServiceClient.getUserByCin(cin);
        } catch (Exception e) {
            try {
                return userServiceClient.getUserByCinFromKycStatus(cin);
            } catch (Exception e2) {
                log.warn("Failed to fetch user info for PDF (CIN: " + cin + ")", e2);
                return null;
            }
        }
    }

    private static byte[] tryDecodePng(String payload) {
        try {
            String b64 = payload.trim();
            int comma = b64.indexOf(',');
            if (b64.startsWith("data:image") && comma >= 0) {
                b64 = b64.substring(comma + 1);
            }
            return Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeText(String s, int maxLen) {
        if (s == null) return "";
        String oneLine = s.replace("\r", " ").replace("\n", " ").trim();
        if (oneLine.length() <= maxLen) return oneLine;
        return oneLine.substring(0, Math.max(0, maxLen)) + "...";
    }

    private static byte[] toBytes(PDDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        return out.toByteArray();
    }

    private static String nvl(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static List<String> wrap(String s, int max) {
        if (s == null) return List.of("");
        if (s.length() <= max) return List.of(s);
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            int end = Math.min(i + max, s.length());
            out.add(s.substring(i, end));
            i = end;
        }
        return out;
    }
}

