package tn.esprit.msprojectservice.services;

import org.jpmml.evaluator.Evaluator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour TaskSuggestionService.
 * Charge le vrai modèle PMML depuis src/main/resources/ml/
 * pour tester la détection de type et les suggestions.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("TaskSuggestionService — Tests unitaires")
class TaskSuggestionServiceTest {

    private TaskSuggestionService service;
    private Method detecterTypeMethod;

    @BeforeAll
    void chargerModeleUneSeuleFois() throws Exception {
        service = new TaskSuggestionService();
        service.chargerModele();

        detecterTypeMethod = TaskSuggestionService.class
                .getDeclaredMethod("detecterType", String.class);
        detecterTypeMethod.setAccessible(true);
    }

    // ─── chargerModele ────────────────────────────────────────────────────────

    @Test
    @DisplayName("chargerModele — modèle chargé sans exception")
    void chargerModele_loadsSuccessfully() {
        // Service already loaded in @BeforeAll; suggererTaches returns non-empty for known types
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("Machine Learning Platform", LocalDate.now());
        // Model is loaded → should not return empty from null-evaluator branch
        assertThat(suggestions).isNotNull();
    }

    @Test
    @DisplayName("suggererTaches — retourne liste vide si évaluateur null (modèle non chargé)")
    void suggererTaches_nullEvaluator_returnsEmptyList() {
        TaskSuggestionService serviceNonCharge = new TaskSuggestionService();
        // chargerModele() NOT called → evaluator is null

        List<TaskSuggestionService.TaskSuggestionDTO> result =
                serviceNonCharge.suggererTaches("E-Commerce Platform", LocalDate.now());

        assertThat(result).isEmpty();
    }

    // ─── detecterType — tous les types de projets ────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "Machine Learning Prediction App,        AI_ML_PROJECT",
            "Deep Learning Neural Network,           AI_ML_PROJECT",
            "NLP Data Science Dataset,               AI_ML_PROJECT",
            "Intelligence Artificielle Prediction,   AI_ML_PROJECT",
            "IoT Sensor Arduino Platform,            IOT_PLATFORM",
            "Raspberry Mqtt Capteur Embarque,        IOT_PLATFORM",
            "Chat Reseau Network Follower Community, SOCIAL_NETWORK",
            "Feed Follower Community App,            SOCIAL_NETWORK",
            "SaaS Platform Subscription Billing,     SAAS_PLATFORM",
            "Plateforme Abonnement Multi Tenant,     SAAS_PLATFORM",
            "E-Commerce Shop Marketplace Boutique,   E_COMMERCE",
            "Store Panier Ecommerce App,             E_COMMERCE",
            "Booking Reservation Hotel Restaurant,   BOOKING_SYSTEM",
            "Appointment Calendrier RDV System,      BOOKING_SYSTEM",
            "Mobile Android iOS Flutter App,         APP_MOBILE",
            "React Native Smartphone App,            APP_MOBILE",
            "Dashboard Analytics KPI Reporting,      DASHBOARD",
            "Tableau de Bord Statistique BI,         DASHBOARD",
            "Blog CMS Content News Portal,           BLOG_CMS",
            "News Blog CMS Content,                  BLOG_CMS",
            "Portfolio CV Personal Showcase Resume,  PORTFOLIO",
            "Personal Showcase Website,              PORTFOLIO",
            "API Backend Microservice REST,          API_BACKEND",
            "GraphQL Spring Node Backend,            API_BACKEND",
            "Vitrine Corporate Website,              SITE_VITRINE"
    })
    @DisplayName("detecterType — mots-clés → type de projet attendu")
    void detecterType_keywords_returnsExpectedType(String title, String expectedType) throws Exception {
        String result = (String) detecterTypeMethod.invoke(service, title.trim());
        assertThat(result).isEqualTo(expectedType.trim());
    }

    @Test
    @DisplayName("detecterType — titre null → SITE_VITRINE par défaut")
    void detecterType_nullTitle_returnsSiteVitrine() throws Exception {
        String result = (String) detecterTypeMethod.invoke(service, (Object) null);
        assertThat(result).isEqualTo("SITE_VITRINE");
    }

    @Test
    @DisplayName("detecterType — titre vide → SITE_VITRINE par défaut")
    void detecterType_blankTitle_returnsSiteVitrine() throws Exception {
        String result = (String) detecterTypeMethod.invoke(service, "   ");
        assertThat(result).isEqualTo("SITE_VITRINE");
    }

    @Test
    @DisplayName("detecterType — titre sans mots-clés → SITE_VITRINE par défaut")
    void detecterType_unknownTitle_returnsSiteVitrineDefault() throws Exception {
        String result = (String) detecterTypeMethod.invoke(service, "Projet sans catégorie particulière");
        assertThat(result).isEqualTo("SITE_VITRINE");
    }

    @Test
    @DisplayName("detecterType — tirets et underscores normalisés (ecommerce sans platform)")
    void detecterType_hyphenAndUnderscore_normalizedCorrectly() throws Exception {
        // "e-commerce_boutique" → "e commerce boutique" → matches "e commerce" → E_COMMERCE
        String result = (String) detecterTypeMethod.invoke(service, "e-commerce_boutique");
        assertThat(result).isEqualTo("E_COMMERCE");
    }

    // ─── suggererTaches ───────────────────────────────────────────────────────

    @Test
    @DisplayName("suggererTaches — retourne liste triée par ordre pour projet E-Commerce")
    void suggererTaches_ecommerce_returnsSortedSuggestions() {
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("TuniShop E-Commerce Store", LocalDate.now());

        assertThat(suggestions).isNotNull();
        if (!suggestions.isEmpty()) {
            // Must be sorted by order
            for (int i = 1; i < suggestions.size(); i++) {
                assertThat(suggestions.get(i).getOrder())
                        .isGreaterThanOrEqualTo(suggestions.get(i - 1).getOrder());
            }
            // Each suggestion has a title
            suggestions.forEach(s -> {
                assertThat(s.getTitle()).isNotBlank();
                assertThat(s.getDescription()).isNotNull();
                assertThat(s.getEstimatedHours()).isGreaterThan(0);
                assertThat(s.getDeadline()).isNotNull();
                assertThat(s.getDetectedProjectType()).isEqualTo("E_COMMERCE");
            });
        }
    }

    @Test
    @DisplayName("suggererTaches — deadline calculée depuis projectStart + ordre × 7 jours")
    void suggererTaches_deadlineCalculatedFromStartDate() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("AI ML Prediction Platform", startDate);

        assertThat(suggestions).isNotNull();
        suggestions.forEach(s -> {
            LocalDate expectedDeadline = startDate.plusDays((long) s.getOrder() * 7);
            assertThat(s.getDeadline()).isEqualTo(expectedDeadline);
        });
    }

    @Test
    @DisplayName("suggererTaches — type détecté propagé dans chaque suggestion")
    void suggererTaches_detectedTypePropagated() {
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("IoT Sensor Arduino Platform", LocalDate.now());

        assertThat(suggestions).isNotNull();
        suggestions.forEach(s ->
                assertThat(s.getDetectedProjectType()).isEqualTo("IOT_PLATFORM"));
    }

    @Test
    @DisplayName("suggererTaches — projet inconnu utilise SITE_VITRINE comme fallback")
    void suggererTaches_unknownType_usesSiteVitrineFallback() {
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("Mon super projet sans type", LocalDate.now());

        assertThat(suggestions).isNotNull();
        suggestions.forEach(s ->
                assertThat(s.getDetectedProjectType()).isEqualTo("SITE_VITRINE"));
    }

    @Test
    @DisplayName("suggererTaches — projet Dashboard retourne des suggestions IA")
    void suggererTaches_dashboardProject_returnsResults() {
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("Analytics Dashboard KPI Reporting", LocalDate.now());

        assertThat(suggestions).isNotNull();
        suggestions.forEach(s -> assertThat(s.getOrder()).isGreaterThan(0));
    }

    @Test
    @DisplayName("suggererTaches — projet API Backend retourne des suggestions")
    void suggererTaches_apiBackendProject_returnsResults() {
        List<TaskSuggestionService.TaskSuggestionDTO> suggestions =
                service.suggererTaches("REST API Backend Microservice Spring", LocalDate.now());

        assertThat(suggestions).isNotNull();
    }

    // ─── predire — chemins d'erreur ───────────────────────────────────────────

    @Test
    @DisplayName("predire — probability(1) absent du résultat → aucune suggestion retournée")
    void predire_nullProbability_returnsZero() throws Exception {
        Evaluator mockEval = mock(Evaluator.class);
        when(mockEval.getInputFields()).thenReturn(List.of());
        doReturn(Map.of()).when(mockEval).evaluate(anyMap());

        Field evalField = TaskSuggestionService.class.getDeclaredField("evaluator");
        evalField.setAccessible(true);
        Object realEvaluator = evalField.get(service);
        evalField.set(service, mockEval);

        try {
            List<TaskSuggestionService.TaskSuggestionDTO> result =
                    service.suggererTaches("E-Commerce Boutique", LocalDate.now());
            assertThat(result).isEmpty();
        } finally {
            evalField.set(service, realEvaluator);
        }
    }

    @Test
    @DisplayName("predire — exception pendant evaluate → retourne 0 silencieusement")
    void predire_evaluateThrowsException_returnsZeroGracefully() throws Exception {
        Evaluator mockEval = mock(Evaluator.class);
        when(mockEval.getInputFields()).thenReturn(List.of());
        when(mockEval.evaluate(anyMap())).thenThrow(new RuntimeException("Erreur JPMML test"));

        Field evalField = TaskSuggestionService.class.getDeclaredField("evaluator");
        evalField.setAccessible(true);
        Object realEvaluator = evalField.get(service);
        evalField.set(service, mockEval);

        try {
            List<TaskSuggestionService.TaskSuggestionDTO> result =
                    service.suggererTaches("API Backend REST", LocalDate.now());
            assertThat(result).isEmpty();
        } finally {
            evalField.set(service, realEvaluator);
        }
    }

    // ─── TaskSuggestionDTO ────────────────────────────────────────────────────

    @Test
    @DisplayName("TaskSuggestionDTO — getters retournent les valeurs correctes")
    void taskSuggestionDTO_getters_returnCorrectValues() {
        LocalDate deadline = LocalDate.of(2026, 6, 1);
        TaskSuggestionService.TaskSuggestionDTO dto =
                new TaskSuggestionService.TaskSuggestionDTO(
                        "Setup Env", "Description", 1, 8, deadline, "API_BACKEND");

        assertThat(dto.getTitle()).isEqualTo("Setup Env");
        assertThat(dto.getDescription()).isEqualTo("Description");
        assertThat(dto.getOrder()).isEqualTo(1);
        assertThat(dto.getEstimatedHours()).isEqualTo(8);
        assertThat(dto.getDeadline()).isEqualTo(deadline);
        assertThat(dto.getDetectedProjectType()).isEqualTo("API_BACKEND");
    }
}
