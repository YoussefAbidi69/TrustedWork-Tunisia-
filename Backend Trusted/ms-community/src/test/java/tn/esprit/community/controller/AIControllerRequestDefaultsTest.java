package tn.esprit.community.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.esprit.community.dto.ai.CourseOutlineRequest;
import tn.esprit.community.service.AIService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIControllerRequestDefaultsTest {

    @Test
    @DisplayName("shouldDefaultMissingFields_whenCourseOutlineRequestHasNulls")
    void shouldDefaultMissingFields_whenCourseOutlineRequestHasNulls() {
        AIService aiService = mock(AIService.class);
        when(aiService.generateCourseOutline("", "")).thenReturn(null);
        AIController controller = new AIController(aiService);

        controller.generateCourseOutline(new CourseOutlineRequest());

        verify(aiService).generateCourseOutline("", "");
    }
}
