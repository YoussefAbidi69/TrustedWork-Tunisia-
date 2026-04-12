package tn.esprit.mscontractservicee.service.document;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.enums.SignatureType;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PdfBoxContractDocumentService implements ContractDocumentService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public byte[] generateContractPdf(Contract contract, List<Milestone> milestones, List<SignatureSigner> signers) {
        if (contract == null) {
            throw new IllegalArgumentException("contract is required");
        }
        List<Milestone> safeMilestones = milestones != null ? milestones : List.of();
        List<SignatureSigner> safeSigners = signers != null ? signers : List.of();

        try (PDDocument doc = new PDDocument()) {
            PDRectangle box = PDRectangle.A4;
            float margin = 48f;
            float leading = 14f;

            PDPage page = new PDPage(box);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = box.getHeight() - margin;

            try {
                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("Contract Document");
                cs.endText();
                y -= (leading * 2);

                List<String> lines = new ArrayList<>();
                lines.add("Reference: " + nvl(contract.getReference()));
                lines.add("Contract ID: " + contract.getId());
                lines.add("Client CIN: " + nvl(contract.getClientCin()));
                lines.add("Freelancer CIN: " + nvl(contract.getFreelancerCin()));
                lines.add("Project: " + nvl(contract.getProjectTitle()));
                lines.add("Total Amount: " + nvl(contract.getMontantTotal()));
                lines.add("Status: " + (contract.getStatus() != null ? contract.getStatus().name() : "null"));
                if (contract.getFinalizedAt() != null) {
                    lines.add("Finalized At: " + DT.format(contract.getFinalizedAt()));
                }
                if (contract.getDateSignature() != null) {
                    lines.add("Signed At: " + DT.format(contract.getDateSignature()));
                }
                lines.add("");
                lines.add("Description:");
                lines.add(nvl(contract.getDescription()));
                lines.add("");
                lines.add("Milestones (" + safeMilestones.size() + "):");
                for (Milestone m : safeMilestones) {
                    String title = (m.getOrdre() != null ? ("#" + m.getOrdre() + " ") : "") + nvl(m.getTitre());
                    lines.add("- " + title + " | amount=" + nvl(m.getMontant()) + " | deadline=" + nvl(m.getDeadline()));
                }

                cs.setFont(PDType1Font.HELVETICA, 11);
                for (String raw : lines) {
                    for (String wrapped : wrap(raw, 95)) {
                        if (y <= margin) {
                            cs.close();
                            page = new PDPage(box);
                            doc.addPage(page);
                            cs = new PDPageContentStream(doc, page);
                            y = box.getHeight() - margin;

                            // Repeat title on new pages (smaller)
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                            cs.newLineAtOffset(margin, y);
                            cs.showText("Contract Document (cont.)");
                            cs.endText();
                            y -= (leading * 2);
                            cs.setFont(PDType1Font.HELVETICA, 11);
                        }

                        cs.beginText();
                        cs.newLineAtOffset(margin, y);
                        cs.showText(wrapped);
                        cs.endText();
                        y -= leading;
                    }
                }
            } finally {
                if (cs != null) {
                    try {
                        cs.close();
                    } catch (Exception ignore) {
                        // best-effort
                    }
                }
            }

            // Add a dedicated signatures page at the end (easiest to render images without layout issues).
            addSignaturesPage(doc, contract, safeSigners);

            return toBytes(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private static void addSignaturesPage(PDDocument doc, Contract contract, List<SignatureSigner> signers) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float margin = 48f;
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 14f;

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.newLineAtOffset(margin, y);
            cs.showText("Signatures");
            cs.endText();
            y -= (leading * 2);

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.newLineAtOffset(margin, y);
            cs.showText("Contract: " + nvl(contract.getReference()) + " (ID " + contract.getId() + ")");
            cs.endText();
            y -= (leading * 2);

            SignatureSigner client = findSigner(signers, SignerRole.CLIENT);
            SignatureSigner freelancer = findSigner(signers, SignerRole.FREELANCER);

            // Two columns
            float col1x = margin;
            float col2x = page.getMediaBox().getWidth() / 2f + 10f;
            float blockTop = y;
            float blockHeight = 220f;

            renderSignerBlock(doc, cs, col1x, blockTop, "CLIENT", client);
            renderSignerBlock(doc, cs, col2x, blockTop, "FREELANCER", freelancer);

            float bottomY = blockTop - blockHeight - (leading * 2);
            if (bottomY < margin) {
                bottomY = margin;
            }

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
            cs.newLineAtOffset(margin, bottomY);
            cs.showText("This is an in-app e-signature record (image/typed signature + audit data stored by the service).");
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

    private static void renderSignerBlock(PDDocument doc,
                                          PDPageContentStream cs,
                                          float x,
                                          float topY,
                                          String label,
                                          SignatureSigner signer) throws Exception {
        float leading = 14f;
        float y = topY;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText("SIGNATURE " + label + ":");
        cs.endText();
        y -= (leading * 1.2f);

        String email = signer != null ? nvl(signer.getSignerEmail()) : "";
        String status = signer != null && signer.getStatus() != null ? signer.getStatus().name() : "PENDING";
        String signedAt = signer != null && signer.getSignedAt() != null ? DT.format(signer.getSignedAt()) : "";

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(x, y);
        cs.showText("Email: " + email);
        cs.endText();
        y -= leading;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(x, y);
        cs.showText("Status: " + status + (signedAt.isBlank() ? "" : (" | Signed at: " + signedAt)));
        cs.endText();
        y -= (leading * 1.5f);

        if (signer == null || signer.getStatus() != SignatureSignerStatus.SIGNED) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            cs.newLineAtOffset(x, y);
            cs.showText("Pending signature");
            cs.endText();
            return;
        }

        // Signature visual
        SignatureType type = signer.getSignatureType();
        String payload = signer.getSignaturePayload();
        if (type == SignatureType.DRAWN && payload != null && !payload.isBlank()) {
            byte[] png = tryDecodePng(payload);
            if (png != null) {
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, png, "signature");
                float maxW = 220f;
                float maxH = 80f;
                float iw = img.getWidth();
                float ih = img.getHeight();
                float scale = Math.min(maxW / iw, maxH / ih);
                float w = iw * scale;
                float h = ih * scale;
                cs.drawImage(img, x, y - h, w, h);
                y -= (h + leading);
                return;
            }
        }

        // Fallback: typed signature or un-decodable image
        cs.beginText();
        cs.setFont(PDType1Font.TIMES_ITALIC, 14);
        cs.newLineAtOffset(x, y);
        cs.showText(payload != null ? safeText(payload, 40) : "");
        cs.endText();
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
        return oneLine.substring(0, Math.max(0, maxLen));
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
