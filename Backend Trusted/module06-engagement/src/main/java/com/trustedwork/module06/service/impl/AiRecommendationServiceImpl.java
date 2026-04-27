package com.trustedwork.module06.service.impl;

import com.trustedwork.module06.entity.Challenge;
import com.trustedwork.module06.entity.Event;
import com.trustedwork.module06.repository.ChallengeRepository;
import com.trustedwork.module06.repository.EventRepository;
import com.trustedwork.module06.service.AiRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final EventRepository eventRepo;
    private final ChallengeRepository challengeRepo;
    private final RestTemplate externalRestTemplate;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private static final String USER_SERVICE_URL = "http://localhost:8081/api/identity/users/";
    private static final String EVENTS_KEY = "events";
    private static final String CHALLENGES_KEY = "challenges";
    private static final String REASON_KEY = "reason";
    private static final String TITLE_KEY = "title";
    private static final String CONTENT_KEY = "content";
    private static final String MESSAGE_KEY = "message";

    @Override
    public Map<String, Object> getSmartRecommendations(Long userId) {
        log.info("--- [SMART] Début pour User {} ---", userId);
        Map<String, Object> profile = fetchUserProfile(userId);
        List<Event> events = eventRepo.findAll();
        List<Challenge> challenges = challengeRepo.findAll();

        if (events.isEmpty() && challenges.isEmpty()) {
            return Map.of(EVENTS_KEY, List.of(), CHALLENGES_KEY, List.of());
        }

        String prompt = buildPrompt(profile, events, challenges);
        String aiJson = callGroqAPI(prompt);
        Map<String, Object> result = parseAiResponse(aiJson, events, challenges);

        // Mode Secours si IA vide
        List<?> evList = (List<?>) result.get(EVENTS_KEY);
        if (evList == null || evList.isEmpty()) {
            log.warn(">>> IA Vide. Mode Secours activé.");
            List<Map<String, Object>> fallback = events.stream()
                    .sorted(Comparator.comparing(Event::getRegisteredCount).reversed())
                    .limit(3)
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", e.getId());
                        m.put(TITLE_KEY, e.getTitle());
                        m.put(REASON_KEY, "Incontournable en ce moment (Recommandé)");
                        return m;
                    })
                    .toList();
            result.put(EVENTS_KEY, fallback);
        }
        return result;
    }

    private Map<String, Object> fetchUserProfile(Long userId) {
        try {
            return externalRestTemplate.getForObject(USER_SERVICE_URL + userId, Map.class);
        } catch (Exception e) {
            log.error(">>> [API USER] Erreur: {}", e.getMessage());
            return Map.of("firstName", "Utilisateur", "location", "Tunisie");
        }
    }

    private String buildPrompt(Map<String, Object> profile, List<Event> events, List<Challenge> challenges) {
        StringBuilder sb = new StringBuilder();
        sb.append("[INST] Tu es une IA experte en recommandation. Analyse le profil utilisateur et suggère exactement 2 événements et 2 challenges parmi la liste fournie.\n\n");

        sb.append("Profil utilisateur:\n");
        sb.append("- Titre: ").append(profile.getOrDefault("headline", "Non spécifié")).append("\n");
        sb.append("- Bio: ").append(profile.getOrDefault("bio", "Non spécifiée")).append("\n");
        sb.append("- Localisation: ").append(profile.get("location")).append("\n\n");

        sb.append("Liste des événements disponibles:\n");
        events.forEach(e -> sb.append("- ID ").append(e.getId()).append(": ").append(e.getTitle()).append("\n"));

        sb.append("\nListe des challenges disponibles:\n");
        challenges.forEach(c -> sb.append("- ID ").append(c.getId()).append(": ").append(c.getTitle()).append("\n"));

        sb.append("\nIMPORTANT: Réponds uniquement avec un objet JSON valide suivant ce format strict, sans texte avant ou après:\n");
        sb.append("{\n");
        sb.append("  \"eventRecommendations\": [{\"id\": ID_ICI, \"reason\": \"Pourquoi cet event?\"}],\n");
        sb.append("  \"challengeRecommendations\": [{\"id\": ID_ICI, \"reason\": \"Pourquoi ce challenge?\"}]\n");
        sb.append("}\n");
        sb.append("[/INST]");

        return sb.toString();
    }

    private String callGroqAPI(String prompt) {
        if (apiKey == null || apiKey.length() < 5) return "";

        log.info(">>> [GROQ] Calling API: {}", apiUrl);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("model", "llama-3.3-70b-versatile"); // Modèle gratuit ultra-rapide
            
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", CONTENT_KEY, prompt));
            bodyMap.put("messages", messages);
            bodyMap.put("max_tokens", 800);
            bodyMap.put("temperature", 0.1);
            
            String jsonRequest = mapper.writeValueAsString(bodyMap);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey.trim());
            headers.set("User-Agent", "TrustedWork-API-Client");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonRequest, headers);

            return executeGroqCall(apiUrl, entity, mapper);
        } catch (Exception e) {
            log.error(">>> [GROQ] Fatal Error: {}", e.getMessage(), e);
        }
        return "";
    }

    private String executeGroqCall(String url, HttpEntity<String> entity, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        try {
            ResponseEntity<String> response = externalRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> resMap = mapper.readValue(response.getBody(), Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) resMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get(MESSAGE_KEY);
                    if (message != null && message.get(CONTENT_KEY) != null) {
                        return cleanJson((String) message.get(CONTENT_KEY));
                    }
                }
            }
        } catch (HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            log.error(">>> [GROQ] API Error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(">>> [GROQ] Processing Error: {}", e.getMessage());
        }
        return "";
    }

    private String cleanJson(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return "";
    }

    private Map<String, Object> parseAiResponse(String json, List<Event> events, List<Challenge> challenges) {
        Map<String, Object> result = new HashMap<>();
        result.put(EVENTS_KEY, new ArrayList<>());
        result.put(CHALLENGES_KEY, new ArrayList<>());
        if (json == null || json.isEmpty()) return result;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> aiMap = mapper.readValue(json, Map.class);

            List<Map<String, Object>> evRecs = (List<Map<String, Object>>) aiMap.get("eventRecommendations");
            if (evRecs != null) {
                result.put(EVENTS_KEY, evRecs.stream().map(rec -> {
                    Long id = Long.valueOf(rec.get("id").toString());
                    Event e = events.stream().filter(ev -> ev.getId().equals(id)).findFirst().orElse(null);
                    return e != null ? Map.of("id", e.getId(), TITLE_KEY, e.getTitle(), REASON_KEY, rec.get(REASON_KEY)) : null;
                }).filter(Objects::nonNull).toList());
            }

            List<Map<String, Object>> chRecs = (List<Map<String, Object>>) aiMap.get("challengeRecommendations");
            if (chRecs != null) {
                result.put(CHALLENGES_KEY, chRecs.stream().map(rec -> {
                    Long id = Long.valueOf(rec.get("id").toString());
                    Challenge c = challenges.stream().filter(ch -> ch.getId().equals(id)).findFirst().orElse(null);
                    return c != null ? Map.of("id", c.getId(), TITLE_KEY, c.getTitle(), REASON_KEY, rec.get(REASON_KEY)) : null;
                }).filter(Objects::nonNull).toList());
            }
        } catch (Exception e) {
            log.error("Parsing Error: {}", e.getMessage());
        }
        return result;
    }
}
