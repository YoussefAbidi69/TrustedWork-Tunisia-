package tn.esprit.freelancerprofileservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.dto.request.AddReviewRequest;
import tn.esprit.freelancerprofileservice.dto.request.ReplyToReviewRequest;
import tn.esprit.freelancerprofileservice.dto.response.ProfileReviewSummaryResponse;
import tn.esprit.freelancerprofileservice.dto.response.ReviewResponse;
import tn.esprit.freelancerprofileservice.enums.ReviewStatus;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.IProfileReviewService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReviewController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IProfileReviewService reviewService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAddReview() throws Exception {
        AddReviewRequest request = new AddReviewRequest();
        request.setClientId(2L);
        request.setRating(5);
        request.setComment("Excellent travail sur ce projet, très professionnel.");

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .clientId(2L)
                .rating(5)
                .comment("Excellent travail sur ce projet, très professionnel.")
                .status(ReviewStatus.VISIBLE)
                .build();

        when(reviewService.addReview(eq(1L), any(AddReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reviews/profiles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent travail sur ce projet, très professionnel."))
                .andExpect(jsonPath("$.status").value("VISIBLE"));

        verify(reviewService).addReview(eq(1L), any(AddReviewRequest.class));
    }

    @Test
    void shouldGetReviews() throws Exception {
        when(reviewService.getVisibleReviews(1L)).thenReturn(List.of(
                ReviewResponse.builder().id(1L).rating(4).comment("Good").build()
        ));

        mockMvc.perform(get("/api/reviews/profiles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].rating").value(4))
                .andExpect(jsonPath("$[0].comment").value("Good"));
    }

    @Test
    void shouldGetAverageRating() throws Exception {
        when(reviewService.getAverageRating(1L)).thenReturn(4.5);

        mockMvc.perform(get("/api/reviews/profiles/1/average"))
                .andExpect(status().isOk())
                .andExpect(content().string("4.5"));
    }

    @Test
    void shouldGetSummary() throws Exception {
        ProfileReviewSummaryResponse summary = ProfileReviewSummaryResponse.builder()
                .profileId(1L)
                .averageRating(4.2)
                .totalReviews(10L)
                .fiveStarCount(6L)
                .fourStarCount(2L)
                .threeStarCount(1L)
                .twoStarCount(1L)
                .oneStarCount(0L)
                .build();

        when(reviewService.getReviewSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/reviews/profiles/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(1))
                .andExpect(jsonPath("$.averageRating").value(4.2))
                .andExpect(jsonPath("$.totalReviews").value(10))
                .andExpect(jsonPath("$.fiveStarCount").value(6));
    }

    @Test
    void shouldReplyToReview() throws Exception {
        ReplyToReviewRequest request = new ReplyToReviewRequest();
        request.setReply("Merci beaucoup pour votre retour.");

        when(reviewService.replyToReview(eq(1L), eq(10L), any(ReplyToReviewRequest.class)))
                .thenReturn(ReviewResponse.builder()
                        .id(1L)
                        .freelancerReply("Merci beaucoup pour votre retour.")
                        .build());

        mockMvc.perform(put("/api/reviews/1/reply")
                        .param("freelancerUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.freelancerReply").value("Merci beaucoup pour votre retour."));

        verify(reviewService).replyToReview(eq(1L), eq(10L), any(ReplyToReviewRequest.class));
    }

    @Test
    void shouldHideReview() throws Exception {
        mockMvc.perform(patch("/api/reviews/1/hide"))
                .andExpect(status().isNoContent());

        verify(reviewService).hideReview(1L);
    }

    @Test
    void shouldDeleteReview() throws Exception {
        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(1L);
    }

    @Test
    void shouldRestoreReview() throws Exception {
        when(reviewService.restoreReview(1L)).thenReturn(
                ReviewResponse.builder()
                        .id(1L)
                        .status(ReviewStatus.VISIBLE)
                        .build()
        );

        mockMvc.perform(put("/api/reviews/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("VISIBLE"));

        verify(reviewService).restoreReview(1L);
    }
}