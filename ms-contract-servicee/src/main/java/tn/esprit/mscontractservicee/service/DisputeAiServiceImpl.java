package tn.esprit.mscontractservicee.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import jakarta.annotation.PostConstruct;
import tn.esprit.mscontractservicee.dto.dispute.DisputeAiRecommendation;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DisputeRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeAiServiceImpl implements IDisputeAiService {

    private final DisputeRepository disputeRepository;
    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final String DEFAULT_RISK_LEVEL = "MEDIUM";

    @PostConstruct
    public void init() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[DisputeAI] ⚠️  Gemini API key is EMPTY. Will use rule-based fallback.");
        } else {
            String preview = geminiApiKey.length() > 8
                    ? geminiApiKey.substring(0, 8) + "..."
                    : "(too short)";
            log.info("[DisputeAI] ✅ Gemini API key loaded: {} (length={})", preview, geminiApiKey.length());
            if (!geminiApiKey.startsWith("AIza")) {
                log.warn("[DisputeAI] ⚠️  Key does NOT start with 'AIza' — this may be an invalid Gemini key. " +
                         "Generate a valid key at: https://aistudio.google.com/apikey");
            }
        }
    }

    @Override
    public DisputeAiRecommendation analyze(Long disputeId, Long adminCin) {
        if (disputeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disputeId is required");
        }

        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Dispute not found with id: " + disputeId));

        Contract contract = contractRepository.findById(dispute.getContractId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + dispute.getContractId()));

        Milestone milestone = dispute.getMilestoneId() != null
                ? milestoneRepository.findById(dispute.getMilestoneId()).orElse(null)
                : null;

        String prompt = buildPrompt(dispute, contract, milestone);

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[DisputeAI] Gemini API key not configured — returning rule-based fallback");
            return buildFallback(dispute, contract, milestone, disputeId);
        }

        try {
            return callGemini(prompt, disputeId);
        } catch (Exception e) {
            log.error("[DisputeAI] ❌ Gemini call failed → falling back to rule-based analysis. Error: {}", e.getMessage());
            return buildFallback(dispute, contract, milestone, disputeId);
        }
    }

    // ─── Prompt Engineering ─────────────────────────────────────────────────────

    private String buildPrompt(Dispute dispute, Contract contract, Milestone milestone) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Tu es un arbitre expert spécialisé dans la résolution de litiges contractuels pour une plateforme freelance B2B en Tunisie. Analyse objectivement ce litige et fournis une recommandation structurée.

                """);

        sb.append("=== CONTEXTE DU CONTRAT ===\n");
        sb.append("Référence contrat: ").append(contract.getReference() != null ? contract.getReference() : "#" + contract.getId()).append("\n");
        sb.append("Description: ").append(nvl(contract.getDescription())).append("\n");
        sb.append("Montant total: ").append(contract.getMontantTotal()).append(" DT\n");
        sb.append("Date début: ").append(contract.getDateDebut()).append("\n");
        sb.append("Date fin: ").append(contract.getDateFin()).append("\n");
        sb.append("Statut contrat: ").append(contract.getStatus()).append("\n\n");

        if (milestone != null) {
            sb.append("=== JALON CONCERNÉ ===\n");
            sb.append("Titre: ").append(nvl(milestone.getTitre())).append("\n");
            sb.append("Description: ").append(nvl(milestone.getDescription())).append("\n");
            sb.append("Montant jalon: ").append(milestone.getMontant()).append(" DT\n");
            sb.append("Statut jalon: ").append(milestone.getStatus()).append("\n");
            sb.append("Deadline: ").append(milestone.getDeadline()).append("\n\n");
        }

        sb.append("=== LITIGE ===\n");
        sb.append("Référence: ").append(dispute.getReference()).append("\n");
        sb.append("Niveau: ").append(milestone != null ? "Litige sur jalon" : "Litige sur contrat entier").append("\n");
        sb.append("Statut actuel: ").append(dispute.getStatus()).append("\n");
        sb.append("Ouvert le: ").append(dispute.getOpenedAt()).append("\n\n");

        sb.append("=== MOTIF DU PLAIGNANT ===\n");
        sb.append(nvl(dispute.getMotif())).append("\n\n");

        sb.append("=== PREUVES DU PLAIGNANT ===\n");
        sb.append(dispute.getPreuvesPlaignant() != null ? dispute.getPreuvesPlaignant() : "Aucune preuve textuelle fournie.").append("\n\n");

        sb.append("=== RÉPONSE / PREUVES DU DÉFENDEUR ===\n");
        sb.append(dispute.getPreuvesDefense() != null ? dispute.getPreuvesDefense() : "Le défendeur n'a pas encore répondu.").append("\n\n");

        sb.append("=== INSTRUCTIONS ===\n");
        sb.append("Réponds UNIQUEMENT en JSON valide avec exactement cette structure (sans markdown, sans balises ```):\n");
        sb.append("{\n");
        sb.append("  \"suggestedDecision\": \"RESOLVED_CLIENT\" | \"RESOLVED_FREELANCER\" | \"SPLIT\" | \"DISMISSED\",\n");
        sb.append("  \"confidenceScore\": 0.0 à 1.0,\n");
        sb.append("  \"riskLevel\": \"LOW\" | \"MEDIUM\" | \"HIGH\",\n");
        sb.append("  \"summary\": \"Résumé neutre du litige en 2-3 phrases\",\n");
        sb.append("  \"reasoning\": \"Explication détaillée de la recommandation (3-5 phrases)\",\n");
        sb.append("  \"suggestedMontantRembourse\": montant numérique ou 0,\n");
        sb.append("  \"suggestedMontantLibere\": montant numérique ou 0,\n");
        sb.append("  \"keyFactors\": [\"facteur1\", \"facteur2\", \"facteur3\"]\n");
        sb.append("}\n\n");
        sb.append("Règles pour les montants:\n");
        sb.append("- RESOLVED_CLIENT → suggestedMontantRembourse = montant du litige, suggestedMontantLibere = 0\n");
        sb.append("- RESOLVED_FREELANCER → suggestedMontantRembourse = 0, suggestedMontantLibere = montant du litige\n");
        sb.append("- SPLIT → répartis équitablement selon la solidité des preuves\n");
        sb.append("- DISMISSED → les deux montants = 0\n");
        sb.append("Le montant de référence est: ").append(milestone != null && milestone.getMontant() != null
                ? milestone.getMontant() + " DT (jalon)" : contract.getMontantTotal() + " DT (contrat total)");

        return sb.toString();
    }

    // ─── Gemini API Call ─────────────────────────────────────────────────────────

    private DisputeAiRecommendation callGemini(String prompt, Long disputeId) throws Exception {
        String url = GEMINI_URL + geminiApiKey;
        log.info("[DisputeAI] Calling Gemini API for dispute #{}", disputeId);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1024
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini returned status: " + response.getStatusCode());
            }
            log.info("[DisputeAI] ✅ Gemini responded successfully for dispute #{}", disputeId);
            return parseGeminiResponse(response.getBody(), disputeId);
        } catch (HttpClientErrorException ex) {
            log.error("[DisputeAI] ❌ Gemini HTTP {} error: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Gemini API error " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString());
        }
    }

    private DisputeAiRecommendation parseGeminiResponse(String responseBody, Long disputeId) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        // Clean any markdown code fences if present
        text = text.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
        }

        JsonNode json = objectMapper.readTree(text);

        List<String> keyFactors = new ArrayList<>();
        JsonNode factors = json.path("keyFactors");
        if (factors.isArray()) {
            factors.forEach(f -> keyFactors.add(f.asText()));
        }

        BigDecimal refundAmount = BigDecimal.valueOf(json.path("suggestedMontantRembourse").asDouble(0));
        BigDecimal releaseAmount = BigDecimal.valueOf(json.path("suggestedMontantLibere").asDouble(0));

        return DisputeAiRecommendation.builder()
                .disputeId(disputeId)
                .suggestedDecision(json.path("suggestedDecision").asText("SPLIT"))
                .confidenceScore(json.path("confidenceScore").asDouble(0.5))
                .riskLevel(json.path("riskLevel").asText(DEFAULT_RISK_LEVEL))
                .summary(json.path("summary").asText(""))
                .reasoning(json.path("reasoning").asText(""))
                .suggestedMontantRembourse(refundAmount)
                .suggestedMontantLibere(releaseAmount)
                .keyFactors(keyFactors)
                .generatedAt(LocalDateTime.now())
                .fallback(false)
                .build();
    }

    // ─── Rule-based Fallback ──────────────────────────────────────────────────────

    private DisputeAiRecommendation buildFallback(Dispute dispute, Contract contract,
                                                    Milestone milestone, Long disputeId) {
        boolean hasDefenseProof = dispute.getPreuvesDefense() != null && !dispute.getPreuvesDefense().isBlank();
        boolean hasPlaignantProof = dispute.getPreuvesPlaignant() != null && !dispute.getPreuvesPlaignant().isBlank();
        boolean noResponse = !hasDefenseProof;

        String decision;
        double confidence;
        String riskLevel;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal release = BigDecimal.ZERO;
        List<String> factors = new ArrayList<>();

        BigDecimal disputeAmount = BigDecimal.ZERO;
        if (milestone != null && milestone.getMontant() != null) {
            disputeAmount = milestone.getMontant();
        } else if (contract.getMontantTotal() != null) {
            disputeAmount = contract.getMontantTotal();
        }

        if (noResponse) {
            decision = "RESOLVED_CLIENT";
            confidence = 0.65;
            riskLevel = DEFAULT_RISK_LEVEL;
            refund = disputeAmount;
            factors.add("Le défendeur n'a pas fourni de réponse");
            factors.add("Absence de preuves de défense");
            factors.add("Règle par défaut : faveur au plaignant sans réponse");
        } else if (hasPlaignantProof && hasDefenseProof) {
            decision = "SPLIT";
            confidence = 0.50;
            riskLevel = "HIGH";
            refund = disputeAmount.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            release = disputeAmount.subtract(refund);
            factors.add("Les deux parties ont fourni des preuves");
            factors.add("Cas ambigu — analyse manuelle recommandée");
            factors.add("Suggestion de partage équitable");
        } else {
            decision = "RESOLVED_FREELANCER";
            confidence = 0.55;
            riskLevel = DEFAULT_RISK_LEVEL;
            release = disputeAmount;
            factors.add("Le défendeur a répondu au litige");
            factors.add("Preuves de défense présentes");
        }

        return DisputeAiRecommendation.builder()
                .disputeId(disputeId)
                .suggestedDecision(decision)
                .confidenceScore(confidence)
                .riskLevel(riskLevel)
                .summary("Analyse basée sur les règles métier (service AI non configuré). " +
                         "Litige " + dispute.getReference() + " concernant le contrat #" + contract.getId() + ".")
                .reasoning("Cette recommandation est générée par un moteur de règles local car la clé API Gemini " +
                           "n'est pas configurée. L'analyse prend en compte la présence ou l'absence de réponse " +
                           "et de preuves des deux parties.")
                .suggestedMontantRembourse(refund)
                .suggestedMontantLibere(release)
                .keyFactors(factors)
                .generatedAt(LocalDateTime.now())
                .fallback(true)
                .build();
    }

    private String nvl(String s) {
        return s != null ? s : "Non spécifié";
    }
}
