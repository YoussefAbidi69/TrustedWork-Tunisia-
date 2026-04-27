package tn.esprit.mscontractservicee.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import tn.esprit.mscontractservicee.dto.ai.ContractAiPromptRequest;
import tn.esprit.mscontractservicee.dto.ai.ContractAiResponse;
import tn.esprit.mscontractservicee.dto.ai.MilestoneAiPromptRequest;
import tn.esprit.mscontractservicee.dto.ai.MilestoneAiResponse;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractAiGenerationServiceImpl implements IContractAiGenerationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${groq.max.concurrent:2}")
    private int groqMaxConcurrent;

    @Value("${groq.retry.max-attempts:3}")
    private int groqRetryMaxAttempts;

    @Value("${groq.retry.initial-backoff-ms:750}")
    private long groqRetryInitialBackoffMs;

    @Value("${groq.retry.max-backoff-ms:8000}")
    private long groqRetryMaxBackoffMs;

    private Semaphore groqConcurrencySemaphore;

    @PostConstruct
    void init() {
        // Simple in-process bulkhead to avoid local request spikes blowing up Groq quotas.
        this.groqConcurrencySemaphore = new Semaphore(Math.max(1, groqMaxConcurrent));
    }

    @Override
    public ContractAiResponse generateContractDraft(ContractAiPromptRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt manquant");
        }
        
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Clé API Groq non configurée sur le serveur");
        }

        String prompt = buildContractPrompt(request.getPrompt());
        try {
            return callGroqForContract(prompt);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ContentAI] Failed to generate contract draft", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la génération avec l'IA: " + e.getMessage());
        }
    }

    @Override
    public MilestoneAiResponse generateMilestoneDraft(MilestoneAiPromptRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt manquant");
        }

        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Clé API Groq non configurée sur le serveur");
        }

        String prompt = buildMilestonePrompt(request);
        try {
            return callGroqForMilestone(prompt);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ContentAI] Failed to generate milestone draft", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la génération avec l'IA: " + e.getMessage());
        }
    }

    private String buildContractPrompt(String userPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert dans la rédaction de contrats freelance professionnels. ");
        sb.append("L'utilisateur te fournit une courte description de son projet. ");
        sb.append("Tu dois extraire ou déduire les informations et me répondre UNIQUEMENT en JSON valide (sans markdown ``` ni commentaire).\n\n");
        sb.append("Voici la demande du client : \"").append(userPrompt).append("\"\n\n");
        
        sb.append("Instruction pour la réponse JSON:\n");
        sb.append("{\n");
        sb.append("  \"projectTitle\": \"Titre court et très pro du projet (ex: Application de Livraison iOS)\",\n");
        sb.append("  \"description\": \"Description technique détaillée d'au moins 3 paragraphes expliquant le projet, les livrables attendus et le contexte. Ajoute du détail.\",\n");
        sb.append("  \"montantTotal\": montant numérique (estime un montant réaliste en dinars tunisiens si non précisé, ex: 1500, 3000, 5000),\n");
        sb.append("  \"dateDebut\": \"Date au format YYYY-MM-DD (mets une date dans environ 1 semaine par défaut)\",\n");
        sb.append("  \"dateFin\": \"Date au format YYYY-MM-DD (ajoute la durée estimée ou demandée à la date de début)\",\n");
        sb.append("  \"slaFreelancerHeures\": nombre d'heures (24, 48 ou 72 selon la complexité),\n");
        sb.append("  \"slaClientJours\": nombre de jours (3, 5 ou 7 selon la complexité)\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String buildMilestonePrompt(MilestoneAiPromptRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un chef de projet expert. Tu vas m'aider à créer un 'jalon' (milestone) pour un contrat existant.\n\n");
        
        sb.append("CONTEXTE DU CONTRAT PARENT :\n");
        sb.append("- Titre du contrat : ").append(req.getContractTitle() != null ? req.getContractTitle() : "Non précisé").append("\n");
        sb.append("- Description générale : ").append(req.getContractDescription() != null ? req.getContractDescription() : "Non précisé").append("\n");
        sb.append("- Budget total restant pour les futurs jalons : ").append(req.getRemainingBudget() != null ? req.getRemainingBudget() + " DT" : "Non spécifié").append("\n");
        sb.append("- Date limite globale du contrat : ").append(req.getContractDeadline() != null ? req.getContractDeadline() : "Non précisée").append("\n");
        
        if (req.getExistingMilestones() != null && !req.getExistingMilestones().isEmpty()) {
            sb.append("- Jalons déjà existants : ").append(String.join(", ", req.getExistingMilestones())).append("\n");
        }

        sb.append("\nDEMANDE DE L'UTILISATEUR POUR CE NOUVEAU JALON :\n");
        sb.append("\"").append(req.getPrompt()).append("\"\n\n");
        
        sb.append("Consignes:\n");
        sb.append("1. Propose un titre pro pour ce jalon.\n");
        sb.append("2. Rédige une description très précise des livrables techniques (bullet points si tu veux).\n");
        sb.append("3. Propose un montant DÉCENT proportionnellement au budget restant (ne dépasse JAMAIS le budget restant).\n");
        sb.append("4. Propose une deadline logique, avant la deadline globale.\n\n");

        sb.append("Réponds UNIQUEMENT en JSON valide (sans markdown ``` ni commentaire) avec ce format:\n");
        sb.append("{\n");
        sb.append("  \"titre\": \"...\",\n");
        sb.append("  \"description\": \"...\",\n");
        sb.append("  \"montant\": montant numérique,\n");
        sb.append("  \"deadline\": \"YYYY-MM-DD\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    // ObjectMapper relaxé pour lire le JSON de l'IA (qui contient souvent des sauts de ligne non echappés)
    private static final ObjectMapper AI_MAPPER = new ObjectMapper()
            .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

    private ContractAiResponse callGroqForContract(String prompt) throws com.fasterxml.jackson.core.JsonProcessingException {
        String json = callGroq(prompt);
        JsonNode root = AI_MAPPER.readTree(json);
        
        return ContractAiResponse.builder()
                .projectTitle(root.path("projectTitle").asText(""))
                .description(root.path("description").asText(""))
                .montantTotal(BigDecimal.valueOf(root.path("montantTotal").asDouble(0)))
                .dateDebut(parseDate(root.path("dateDebut").asText()))
                .dateFin(parseDate(root.path("dateFin").asText()))
                .slaFreelancerHeures(root.path("slaFreelancerHeures").asInt(24))
                .slaClientJours(root.path("slaClientJours").asInt(7))
                .build();
    }

    private MilestoneAiResponse callGroqForMilestone(String prompt) throws com.fasterxml.jackson.core.JsonProcessingException {
        String json = callGroq(prompt);
        JsonNode root = AI_MAPPER.readTree(json);

        return MilestoneAiResponse.builder()
                .titre(root.path("titre").asText(""))
                .description(root.path("description").asText(""))
                .montant(BigDecimal.valueOf(root.path("montant").asDouble(0)))
                .deadline(parseDate(root.path("deadline").asText()))
                .build();
    }

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private String callGroq(String prompt) throws com.fasterxml.jackson.core.JsonProcessingException {
        HttpEntity<Map<String, Object>> entity = buildGroqEntity(resolveGroqModel(), prompt);
        ensureGroqSemaphoreInitialized();
        acquireGroqPermitOrThrow();
        try {
            return executeGroqWithRetry(entity);
        } finally {
            groqConcurrencySemaphore.release();
        }
    }

    private String resolveGroqModel() {
        String model = (groqModel == null || groqModel.isBlank())
                ? "llama-3.3-70b-versatile"
                : groqModel.trim();
        // Fallback to llama3 if user kept the old gemini model name
        if (model.contains("gemini")) {
            return "llama-3.3-70b-versatile";
        }
        return model;
    }

    private HttpEntity<Map<String, Object>> buildGroqEntity(String model, String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7,
                "max_tokens", 1024
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);
        return new HttpEntity<>(requestBody, headers);
    }

    private void ensureGroqSemaphoreInitialized() {
        // Defensive: in unit tests or non-Spring usage, @PostConstruct may not run.
        if (groqConcurrencySemaphore == null) {
            this.groqConcurrencySemaphore = new Semaphore(Math.max(1, groqMaxConcurrent));
        }
    }

    private void acquireGroqPermitOrThrow() {
        boolean acquired;
        try {
            acquired = groqConcurrencySemaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service IA indisponible (interruption)");
        }

        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de requetes IA en parallele. Reessayez.");
        }
    }

    private String executeGroqWithRetry(HttpEntity<Map<String, Object>> entity) throws com.fasterxml.jackson.core.JsonProcessingException {
        int maxAttempts = Math.max(1, groqRetryMaxAttempts);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(GROQ_URL, HttpMethod.POST, entity, String.class);
                return extractTextFromGroqResponse(response);
            } catch (HttpStatusCodeException ex) {
                int status = ex.getStatusCode().value();
                String body = ex.getResponseBodyAsString();
                log.warn("[ContentAI] Groq HTTP {} attempt {}/{}: {}", status, attempt, maxAttempts, body);

                if (attempt < maxAttempts && isRetryableStatus(status)) {
                    long sleepMs = computeRetryDelayMs(attempt, ex.getResponseHeaders(), body);
                    sleepQuietly(sleepMs);
                } else {
                    throw translateGroqHttpException(status, ex.getResponseHeaders(), body);
                }
            } catch (ResourceAccessException ex) {
                log.warn("[ContentAI] Groq network error attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
                if (attempt < maxAttempts) {
                    long sleepMs = computeRetryDelayMs(attempt, null, null);
                    sleepQuietly(sleepMs);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur reseau lors de l'appel IA (Groq)");
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service IA indisponible");
    }

    private String extractTextFromGroqResponse(ResponseEntity<String> response) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reponse IA invalide (aucune reponse)");
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Reponse IA invalide (status=" + response.getStatusCode() + ")");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reponse IA invalide (choices manquants)");
        }

        String text = choices.get(0).path("message").path("content").asText("");
        text = stripMarkdownCodeFence(text);
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Reponse IA vide");
        }
        return text;
    }

    private String stripMarkdownCodeFence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            return trimmed.replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();
        }
        return trimmed;
    }

    private ResponseStatusException translateGroqHttpException(int status, HttpHeaders headers, String body) {
        if (status == 429) {
            Long retryAfterMs = firstNonNull(parseRetryAfterMs(headers), parseRetryDelayMsFromBody(body));
            String msg = buildGroqTooManyRequestsMessage(body, retryAfterMs);
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, msg);
        }
        // Pour les autres 4xx/5xx en provenance du provider, remonter une 502 cote client.
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur IA (Groq) HTTP " + status);
    }

    private String buildGroqTooManyRequestsMessage(String body, Long retryAfterMs) {
        boolean quotaExceeded = body != null && body.contains("Quota exceeded");
        boolean quotaDisabledOrZero = body != null && body.contains("limit: 0");

        String reason;
        if (quotaDisabledOrZero) {
            reason = "Quota Groq a 0 (plan non active ou quotas depasses pour ce projet/cle).";
        } else if (quotaExceeded) {
            reason = "Quota Groq depasse (verifiez plan et quotas).";
        } else {
            reason = "Service IA (Groq) temporairement surcharge.";
        }

        return reason + " Reessayez"
                + (retryAfterMs != null && retryAfterMs > 0
                ? " dans environ " + Math.max(1, retryAfterMs / 1000) + "s."
                : " plus tard.");
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status == 408 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private long computeRetryDelayMs(int attempt, HttpHeaders responseHeaders, String responseBody) {
        Long retryAfterMs = firstNonNull(parseRetryAfterMs(responseHeaders), parseRetryDelayMsFromBody(responseBody));
        if (retryAfterMs != null && retryAfterMs > 0) {
            // Small jitter so multiple threads don't retry at the exact same moment.
            return retryAfterMs + ThreadLocalRandom.current().nextLong(0, 250);
        }

        // Exponential backoff with cap + jitter.
        long base = Math.max(0, groqRetryInitialBackoffMs);
        long cap = Math.max(base, groqRetryMaxBackoffMs);

        long exp;
        try {
            exp = Math.multiplyExact(base, 1L << Math.max(0, attempt - 1));
        } catch (ArithmeticException overflow) {
            exp = cap;
        }

        long backoff = Math.min(exp, cap);
        return backoff + ThreadLocalRandom.current().nextLong(0, 250);
    }

    private Long parseRetryAfterMs(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }

        String trimmed = retryAfter.trim();
        try {
            long seconds = Long.parseLong(trimmed);
            return Math.max(0, seconds * 1000);
        } catch (NumberFormatException ignored) {
            // fallback to HTTP-date format
        }

        try {
            ZonedDateTime date = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
            long ms = Duration.between(Instant.now(), date.toInstant()).toMillis();
            return Math.max(0, ms);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseRetryDelayMsFromBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode details = root.path("error").path("details");
            if (!details.isArray()) {
                return null;
            }
            for (JsonNode d : details) {
                String type = d.path("@type").asText("");
                if (!"type.googleapis.com/google.rpc.RetryInfo".equals(type)) {
                    continue;
                }
                String retryDelay = d.path("retryDelay").asText("");
                // Example: "24s" (seconds). Keep parsing simple.
                if (retryDelay.endsWith("s")) {
                    String secondsStr = retryDelay.substring(0, retryDelay.length() - 1).trim();
                    long seconds = Long.parseLong(secondsStr);
                    return Math.max(0, seconds * 1000);
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private void sleepQuietly(long sleepMs) {
        if (sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equalsIgnoreCase("null")) {
            return LocalDate.now().plusDays(7); // default 
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now().plusDays(7);
        }
    }
}
