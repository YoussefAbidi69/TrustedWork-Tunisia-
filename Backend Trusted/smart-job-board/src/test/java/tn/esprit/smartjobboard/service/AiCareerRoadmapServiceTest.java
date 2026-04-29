package tn.esprit.smartjobboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import tn.esprit.smartjobboard.dto.CareerInsightResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("AiCareerRoadmapService")
class AiCareerRoadmapServiceTest {

    private AiCareerRoadmapService service;
    private RestTemplate restTemplateMock;

    @BeforeEach
    void setUp() {
        service = new AiCareerRoadmapService();
        restTemplateMock = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplateMock);
    }

    @Test
    @DisplayName("should use fallback when currentSkills is null or empty")
    void fallbackEmptySkills() {
        ReflectionTestUtils.setField(service, "openAiKey", "valid_key");

        CareerInsightResponse res1 = service.generateRoadmap(null);
        CareerInsightResponse res2 = service.generateRoadmap(List.of());

        assertThat(res1.getTargetRole()).contains("Full Stack");
        assertThat(res2.getTargetRole()).contains("Full Stack");
        verifyNoInteractions(restTemplateMock);
    }

    @Test
    @DisplayName("should use fallback when openAiKey is not configured")
    void fallbackNoKey() {
        ReflectionTestUtils.setField(service, "openAiKey", null);

        CareerInsightResponse res = service.generateRoadmap(List.of("Java"));

        assertThat(res.getTargetRole()).contains("Full Stack");
        verifyNoInteractions(restTemplateMock);
    }

    @Test
    @DisplayName("should call OpenAI and parse response on success")
    void openAiSuccess() throws Exception {
        ReflectionTestUtils.setField(service, "openAiKey", "test-key");

        String mockJson = """
                {
                  "targetRole": "Cloud Architect",
                  "currentLevel": "Mid",
                  "totalWeeks": 12,
                  "totalIncomeBoost": 20.0,
                  "currentRate": 30.0,
                  "projectedRate": 50.0,
                  "difficulty": "Hard",
                  "steps": []
                }
                """;

        Map<String, Object> mockResponse = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", mockJson))
                )
        );

        when(restTemplateMock.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(mockResponse);

        CareerInsightResponse result = service.generateRoadmap(List.of("Java", "AWS"));

        assertThat(result.getTargetRole()).isEqualTo("Cloud Architect");
        assertThat(result.getTotalWeeks()).isEqualTo(12);
        verify(restTemplateMock).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    @DisplayName("should strip markdown fences from OpenAI response")
    void markdownStripping() throws Exception {
        ReflectionTestUtils.setField(service, "openAiKey", "test-key");

        String mockJson = "```json\n{\"targetRole\": \"DevOps\"}\n```";

        Map<String, Object> mockResponse = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", mockJson))
                )
        );

        when(restTemplateMock.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(mockResponse);

        CareerInsightResponse result = service.generateRoadmap(List.of("Docker"));

        assertThat(result.getTargetRole()).isEqualTo("DevOps");
    }

    @Test
    @DisplayName("should fallback when OpenAI throws exception")
    void openAiException() {
        ReflectionTestUtils.setField(service, "openAiKey", "test-key");

        when(restTemplateMock.postForObject(anyString(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("API Down"));

        CareerInsightResponse result = service.generateRoadmap(List.of("Java"));

        assertThat(result.getTargetRole()).contains("Full Stack"); // the fallback response
    }

    @Test
    @DisplayName("should fallback when OpenAI returns invalid payload structure")
    void openAiInvalidPayload() {
        ReflectionTestUtils.setField(service, "openAiKey", "test-key");

        // Missing "choices"
        Map<String, Object> badResponse = Map.of("error", "something went wrong");

        when(restTemplateMock.postForObject(anyString(), any(), eq(Map.class)))
                .thenReturn(badResponse);

        CareerInsightResponse result = service.generateRoadmap(List.of("Java"));

        assertThat(result.getTargetRole()).contains("Full Stack");
    }
}
