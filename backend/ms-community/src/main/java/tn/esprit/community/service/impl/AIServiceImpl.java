package tn.esprit.community.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.community.dto.ai.CourseOutlineResponse;
import tn.esprit.community.dto.ai.QuizQuestion;
import tn.esprit.community.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AIServiceImpl implements AIService {

    @Value("${app.ai.api-url}")
    private String apiUrl;

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);
    private static final String KEY_CONTENT = "content";

    public AIServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private String callLLM(String systemPrompt, String userMessage, boolean jsonMode) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE") || apiKey.equals("YOUR_OPENAI_API_KEY_HERE")) {
            logger.warn("AI API key is missing or set to a default placeholder. Please configure app.ai.api-key in application.properties.");
            return "";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", KEY_CONTENT, systemPrompt));
            messages.add(Map.of("role", "user", KEY_CONTENT, userMessage));
            requestBody.put("messages", messages);

            if (jsonMode) {
                requestBody.put("response_format", Map.of("type", "json_object"));
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get(KEY_CONTENT);
                }
            }
            return "";
        } catch (org.springframework.web.client.RestClientResponseException e) {
            int status = e.getStatusCode().value();
            logger.error("AI API Error (status={}): {}", status, e.getResponseBodyAsString(), e);
            return "";
        } catch (Exception e) {
            logger.error("Error calling AI API", e);
            return "";
        }
    }

    @Override
    public CourseOutlineResponse generateCourseOutline(String topic, String level) {
        String systemPrompt = "You are an expert course creator. Generate a structured course outline based on the topic and level provided. Output ONLY valid JSON in this format: {\"topic\": \"...\", \"level\": \"...\", \"sections\": [{\"title\": \"...\", \"lessons\": [\"...\", \"...\"]}]}";
        String userMessage = "Create an outline for a course about: " + topic + " at " + level + " level.";

        String jsonResult = callLLM(systemPrompt, userMessage, true);

        try {
            if (jsonResult != null && !jsonResult.isBlank()) {
                return objectMapper.readValue(jsonResult, CourseOutlineResponse.class);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse course outline JSON from AI response", e);
        }

        return CourseOutlineResponse.builder().topic(topic).level(level).sections(new ArrayList<>()).build();
    }

    @Override
    public List<QuizQuestion> generateQuiz(String lessonContent) {
        String systemPrompt = "You are an expert tutor. Generate 3 multiple choice questions based on the lesson content provided. Output ONLY valid JSON in this format: {\"questions\": [{\"question\": \"...\", \"options\": [\"...\", \"...\", \"...\", \"...\"], \"correctIndex\": 0}]}";
        String userMessage = "Lesson content: " + lessonContent;

        String jsonResult = callLLM(systemPrompt, userMessage, true);

        try {
            if (jsonResult != null && !jsonResult.isBlank()) {
                Map<String, List<QuizQuestion>> result = objectMapper.readValue(jsonResult, new TypeReference<Map<String, List<QuizQuestion>>>() {});
                if (result.containsKey("questions")) {
                    return result.get("questions");
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse quiz JSON from AI response", e);
        }

        return new ArrayList<>();
    }

    @Override
    public String summarizeLesson(String lessonContent) {
        String systemPrompt = "You are an expert summarizer. Provide a concise summary of the provided lesson content.";
        String userMessage = "Summarize the following: " + lessonContent;

        return callLLM(systemPrompt, userMessage, false).trim();
    }

    @Override
    public String tutorAnswer(String courseContent, String question) {
        String systemPrompt = "You are a helpful AI co-pilot for a course creator. Complete the instruction or answer the question using the provided course content as context. Be concise and educational.";
        String userMessage = "Course context: " + courseContent + "\n\nUser request: " + question;

        return callLLM(systemPrompt, userMessage, false).trim();
    }
}
