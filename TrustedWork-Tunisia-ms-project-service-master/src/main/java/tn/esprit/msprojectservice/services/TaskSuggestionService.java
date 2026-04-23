package tn.esprit.msprojectservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jpmml.evaluator.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * Service de suggestion de tâches basé sur le modèle RandomForest PMML.
 *
 * Fichiers requis dans src/main/resources/ml/ :
 *   - task_suggester.pmml
 *   - task_suggester_mappings.json
 */
@Slf4j
@Service
public class TaskSuggestionService {

    private Evaluator evaluator;
    private Map<String, Integer>              projectTypeEncoding;
    private Map<Integer, String>              taskTitleDecoding;
    private Map<String, Map<String, Object>>  taskDetails;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void chargerModele() {
        try {
            // Chargement PMML
            InputStream pmmlStream = new ClassPathResource("ml/task_suggester.pmml").getInputStream();
            this.evaluator = new LoadingModelEvaluatorBuilder().load(pmmlStream).build();
            this.evaluator.verify();

            // Chargement mappings JSON
            InputStream jsonStream = new ClassPathResource("ml/task_suggester_mappings.json").getInputStream();
            Map<String, Object> mappings = new ObjectMapper().readValue(jsonStream, Map.class);

            this.projectTypeEncoding = (Map<String, Integer>) mappings.get("project_type_encoding");

            // Les clés du JSON sont des String ("0","1",...) → convertir en Integer
            Map<String, Object> rawDecoding = (Map<String, Object>) mappings.get("task_title_decoding");
            this.taskTitleDecoding = new HashMap<>();
            rawDecoding.forEach((k, v) -> taskTitleDecoding.put(Integer.parseInt(k), (String) v));

            this.taskDetails = (Map<String, Map<String, Object>>) mappings.get("task_details");

            log.info("✅ [TaskSuggestionService] Modèle PMML chargé — {} types de projets",
                    projectTypeEncoding.size());

        } catch (Exception e) {
            log.error("❌ [TaskSuggestionService] Erreur chargement modèle : {}", e.getMessage());
        }
    }

    /**
     * Retourne la liste ordonnée des tâches suggérées pour un projet.
     *
     * @param projectTitle  Titre du projet (ex: "TuniShop E-Commerce")
     * @param projectStart  Date de début du projet (pour calculer les deadlines)
     * @return Liste de TaskSuggestionDTO triée par ordre logique
     */
    public List<TaskSuggestionDTO> suggererTaches(String projectTitle, LocalDate projectStart) {
        if (evaluator == null) {
            log.error("Modèle non disponible");
            return Collections.emptyList();
        }

        String projectType = detecterType(projectTitle);
        Integer projectTypeCode = projectTypeEncoding.getOrDefault(projectType,
                projectTypeEncoding.get("SITE_VITRINE"));

        log.info("🔍 Titre='{}' → Type='{}' (code={})", projectTitle, projectType, projectTypeCode);

        List<TaskSuggestionDTO> suggestions = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : taskTitleDecoding.entrySet()) {
            int    taskCode = entry.getKey();
            String taskName = entry.getValue();

            int predicted = predire(projectTypeCode, taskCode);

            if (predicted == 1 && taskDetails.containsKey(taskName)) {
                Map<String, Object> details = taskDetails.get(taskName);
                int       order          = (Integer) details.get("order");
                int       estimatedHours = (Integer) details.get("estimatedHours");
                String    description    = (String)  details.get("description");
                LocalDate deadline       = projectStart.plusDays((long) order * 7);

                suggestions.add(new TaskSuggestionDTO(
                        taskName, description, order, estimatedHours, deadline, projectType
                ));
            }
        }

        suggestions.sort(Comparator.comparingInt(TaskSuggestionDTO::getOrder));
        return suggestions;
    }

    /**
     * Appelle le modèle PMML pour prédire si une tâche est suggérée (1) ou non (0).
     * Compatible jpmml-evaluator 1.7.x
     */
    private int predire(int projectTypeCode, int taskTitleCode) {
        try {
            Map<String, Object> inputArgs = new HashMap<>();
            inputArgs.put("project_type_enc", (double) projectTypeCode);
            inputArgs.put("task_title_enc",   (double) taskTitleCode);

            Map<String, FieldValue> args = new LinkedHashMap<>();
            for (InputField inputField : evaluator.getInputFields()) {
                String fieldName = inputField.getName();
                Object value     = inputArgs.get(fieldName);
                if (value != null) {
                    args.put(fieldName, inputField.prepare(value));
                }
            }

            Map<String, ?> result = evaluator.evaluate(args);

            // Le modèle expose probability(1) comme OutputField explicite
            Object prob1 = result.get("probability(1)");
            if (prob1 == null) {
                log.warn("probability(1) absent du résultat pour task={}", taskTitleCode);
                return 0;
            }
            return ((Double) prob1) >= 0.5 ? 1 : 0;

        } catch (Exception e) {
            log.warn("Erreur prédiction task={} : {}", taskTitleCode, e.getMessage());
            return 0;
        }
    }

    /**
     * Détecte le type du projet via mots-clés dans le titre (NLP simple).
     */
    private String detecterType(String titre) {
        if (titre == null || titre.isBlank()) return "SITE_VITRINE";
        String t = titre.toLowerCase().replace("-", " ").replace("_", " ");

        if (contient(t, "ia", "ml", "machine learning", "deep learning", "neural",
                "intelligence artificielle", "prediction", "nlp", "data science", "dataset"))
            return "AI_ML_PROJECT";
        if (contient(t, "iot", "capteur", "sensor", "arduino", "raspberry", "mqtt", "embarque"))
            return "IOT_PLATFORM";
        if (contient(t, "social", "reseau", "network", "community", "feed", "follower", "chat"))
            return "SOCIAL_NETWORK";
        if (contient(t, "saas", "platform", "plateforme", "subscription", "abonnement", "billing", "multi tenant"))
            return "SAAS_PLATFORM";
        if (contient(t, "ecommerce", "e commerce", "shop", "store", "boutique", "marketplace", "panier"))
            return "E_COMMERCE";
        if (contient(t, "booking", "reservation", "rdv", "appointment", "calendrier", "hotel", "restaurant"))
            return "BOOKING_SYSTEM";
        if (contient(t, "mobile", "android", "ios", "flutter", "react native", "smartphone"))
            return "APP_MOBILE";
        if (contient(t, "dashboard", "analytics", "tableau de bord", "reporting", "kpi", "statistique"))
            return "DASHBOARD";
        if (contient(t, "blog", "cms", "content", "article", "editorial", "news", "media"))
            return "BLOG_CMS";
        if (contient(t, "portfolio", "cv", "personal", "showcase", "resume"))
            return "PORTFOLIO";
        if (contient(t, "api", "backend", "microservice", "rest", "graphql", "spring", "node"))
            return "API_BACKEND";
        return "SITE_VITRINE";
    }

    private boolean contient(String texte, String... mots) {
        for (String mot : mots)
            if (texte.contains(mot)) return true;
        return false;
    }

    // ── DTO ───────────────────────────────────────────────────────

    public static class TaskSuggestionDTO {
        private final String    title;
        private final String    description;
        private final int       order;
        private final int       estimatedHours;
        private final LocalDate deadline;
        private final String    detectedProjectType;

        public TaskSuggestionDTO(String title, String description, int order,
                                 int estimatedHours, LocalDate deadline, String detectedProjectType) {
            this.title               = title;
            this.description         = description;
            this.order               = order;
            this.estimatedHours      = estimatedHours;
            this.deadline            = deadline;
            this.detectedProjectType = detectedProjectType;
        }

        public String    getTitle()               { return title; }
        public String    getDescription()         { return description; }
        public int       getOrder()               { return order; }
        public int       getEstimatedHours()      { return estimatedHours; }
        public LocalDate getDeadline()            { return deadline; }
        public String    getDetectedProjectType() { return detectedProjectType; }
    }
}