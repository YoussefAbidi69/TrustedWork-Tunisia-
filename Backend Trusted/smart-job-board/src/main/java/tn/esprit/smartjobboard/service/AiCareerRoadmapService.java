package tn.esprit.smartjobboard.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.smartjobboard.dto.CareerInsightResponse;
import tn.esprit.smartjobboard.dto.CareerRoadmapStepDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiCareerRoadmapService {

    @Value("${ai.openai.api-key:}")
    private String openAiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public AiCareerRoadmapService() {
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CareerInsightResponse generateRoadmap(List<String> currentSkills) {
        if (currentSkills == null || currentSkills.isEmpty()) {
            return fallbackMock(currentSkills);
        }

        if (openAiKey == null || openAiKey.isBlank()) {
            log.warn("No OpenAI API key configured — using local mock.");
            return fallbackMock(currentSkills);
        }

        try {
            return callOpenAi(currentSkills);
        } catch (Exception e) {
            log.error("OpenAI call failed, falling back to mock. Reason: {}", e.getMessage(), e);
            return fallbackMock(currentSkills);
        }
    }

    @SuppressWarnings("unchecked")
    private CareerInsightResponse callOpenAi(List<String> currentSkills) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String skillList = String.join(", ", currentSkills);

        String systemPrompt = "You are a senior career coach and tech industry expert. "
                + "Always respond with ONLY raw JSON. No markdown, no backticks, no explanation. "
                + "The JSON must have this exact shape:\n"
                + "{\n"
                + "  \"targetRole\": \"string\",\n"
                + "  \"currentLevel\": \"string\",\n"
                + "  \"totalWeeks\": number,\n"
                + "  \"totalIncomeBoost\": number,\n"
                + "  \"currentRate\": number,\n"
                + "  \"projectedRate\": number,\n"
                + "  \"difficulty\": \"string\",\n"
                + "  \"steps\": [\n"
                + "    {\n"
                + "      \"id\": number,\n"
                + "      \"title\": \"string\",\n"
                + "      \"description\": \"string\",\n"
                + "      \"difficultyLevel\": \"string\",\n"
                + "      \"estimatedWeeks\": number,\n"
                + "      \"hoursPerDay\": number,\n"
                + "      \"incomeBoostThisStep\": number,\n"
                + "      \"microCurriculum\": [\n"
                + "        { \"week\": number, \"focus\": \"string\" }\n"
                + "      ],\n"
                + "      \"resources\": [\"string\"],\n"
                + "      \"portfolioProject\": \"string\",\n"
                + "      \"prerequisiteSkills\": [\"string\"],\n"
                + "      \"skillsUnlocked\": [\"string\"],\n"
                + "      \"demandLevel\": \"string\",\n"
                + "      \"color\": \"#E8735A\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "Make every field detailed and specific to the actual skill. microCurriculum must have real weekly content not placeholders. portfolioProject must be impressive and specific. resources must be real named courses and documentation that actually exist.";

        String userPrompt = "The freelancer's current skills are: " + skillList + ". "
                + "Generate a highly detailed 3-to-5 step career roadmap.";

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("temperature", 0.7);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Calling OpenAI for career roadmap with skills: {}", skillList);
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("OpenAI returned null or missing 'choices'");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            throw new RuntimeException("OpenAI returned empty choices array");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new RuntimeException("OpenAI choice has no message");
        }

        String content = (String) message.get("content");
        log.info("OpenAI raw response content: {}", content);
        content = extractJson(content);

        return mapper.readValue(content, CareerInsightResponse.class);
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        content = content.trim();
        // Strip markdown code fences if LLM ignored system prompt
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private CareerInsightResponse fallbackMock(List<String> currentSkills) {
        String baseSkill = (currentSkills != null && !currentSkills.isEmpty())
                ? currentSkills.get(0) : "Software";

        return CareerInsightResponse.builder()
                .targetRole("Senior Full Stack Developer")
                .currentLevel("Junior Developer")
                .totalWeeks(24)
                .totalIncomeBoost(45.0)
                .currentRate(25.0)
                .projectedRate(45.0)
                .difficulty("Moderate")
                .steps(List.of(
                        CareerRoadmapStepDto.builder()
                                .id(1)
                                .title("Master TypeScript")
                                .description("TypeScript is now the default for every serious JavaScript project. Freelancers without it are filtered out of 60% of high-paying contracts.")
                                .difficultyLevel("Intermediate")
                                .estimatedWeeks(4)
                                .hoursPerDay(2)
                                .incomeBoostThisStep(8.0)
                                .microCurriculum(List.of(
                                        tn.esprit.smartjobboard.dto.MicroCurriculumDto.builder().week(1).focus("Types, interfaces, enums, and basic generics").build(),
                                        tn.esprit.smartjobboard.dto.MicroCurriculumDto.builder().week(2).focus("Advanced types, utility types, conditional types").build(),
                                        tn.esprit.smartjobboard.dto.MicroCurriculumDto.builder().week(3).focus("TypeScript with React, typed hooks, typed Redux").build(),
                                        tn.esprit.smartjobboard.dto.MicroCurriculumDto.builder().week(4).focus("Strict mode, monorepo config, declaration files").build()
                                ))
                                .resources(List.of("TypeScript official handbook — typescriptlang.org/docs", "Total TypeScript by Matt Pocock — totaltypescript.com", "Execute Program TypeScript course — executeprogram.com"))
                                .portfolioProject("Migrate a real JavaScript React app to TypeScript strict mode with full type coverage, custom utility types, and a typed REST API client using generics")
                                .prerequisiteSkills(List.of("JavaScript ES6+", "React basics"))
                                .skillsUnlocked(List.of("Typed APIs", "Enterprise Codebases", "Framework Internals"))
                                .demandLevel("Very High")
                                .color("#E8735A")
                                .build()
                ))
                .build();
    }
}
