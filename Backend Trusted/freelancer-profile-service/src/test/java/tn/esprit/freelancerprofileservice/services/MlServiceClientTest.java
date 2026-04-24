package tn.esprit.freelancerprofileservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.repositories.CertificationRepository;
import tn.esprit.freelancerprofileservice.repositories.PortfolioItemRepository;
import tn.esprit.freelancerprofileservice.repositories.ProfileReviewRepository;
import tn.esprit.freelancerprofileservice.repositories.SkillRepository;
import tn.esprit.freelancerprofileservice.repositories.WorkExperienceRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlServiceClientTest {

    @Mock private RestTemplate restTemplate;
    @Mock private SkillRepository skillRepository;
    @Mock private PortfolioItemRepository portfolioItemRepository;
    @Mock private CertificationRepository certificationRepository;
    @Mock private WorkExperienceRepository workExperienceRepository;
    @Mock private ProfileReviewRepository profileReviewRepository;

    @InjectMocks
    private MlServiceClient mlServiceClient;

    // ─── predictSentiment ───────────────────────────────────────────────────

    @Test
    void predictSentiment_shouldReturnResult_whenMlServiceResponds() {
        Map<String, Object> body = Map.of("sentiment", "POSITIVE", "score", 0.95);

        doReturn(ResponseEntity.ok(body))
                .when(restTemplate).postForEntity(anyString(), any(), any());

        MlServiceClient.SentimentResult result = mlServiceClient.predictSentiment("Great work!");

        assertThat(result).isNotNull();
        assertThat(result.sentiment()).isEqualTo("POSITIVE");
        assertThat(result.score()).isEqualTo(0.95);
    }

    @Test
    void predictSentiment_shouldReturnUnknown_whenMlServiceUnavailable() {
        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        MlServiceClient.SentimentResult result = mlServiceClient.predictSentiment("some comment");

        assertThat(result).isNotNull();
        assertThat(result.sentiment()).isEqualTo("UNKNOWN");
        assertThat(result.score()).isEqualTo(0.0);
    }

    // ─── predictTrustScore ──────────────────────────────────────────────────

    @Test
    void predictTrustScore_shouldReturnResult_whenMlServiceResponds() {
        FreelancerProfile profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setCompletenessScore(80);

        when(skillRepository.countByProfileId(1L)).thenReturn(5L);
        when(skillRepository.sumEndorsementsByProfileId(1L)).thenReturn(10L);
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(3L);
        when(certificationRepository.countByProfileId(1L)).thenReturn(2L);
        when(workExperienceRepository.sumMonthsByProfileId(1L)).thenReturn(24L);
        when(profileReviewRepository.countByProfileId(1L)).thenReturn(8L);
        when(profileReviewRepository.avgRatingByProfileId(1L)).thenReturn(4.5);

        Map<String, Object> body = Map.of("level", "HIGH", "confidence", 0.88);

        doReturn(ResponseEntity.ok(body))
                .when(restTemplate).postForEntity(anyString(), any(), any());

        MlServiceClient.TrustScoreResult result =
                mlServiceClient.predictTrustScore(profile, true, true);

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("HIGH");
        assertThat(result.confidence()).isEqualTo(0.88);
    }

    @Test
    void predictTrustScore_shouldReturnUnknown_whenMlServiceUnavailable() {
        FreelancerProfile profile = new FreelancerProfile();
        profile.setId(1L);
        profile.setCompletenessScore(50);

        when(skillRepository.countByProfileId(1L)).thenReturn(2L);
        when(skillRepository.sumEndorsementsByProfileId(1L)).thenReturn(0L);
        when(portfolioItemRepository.countByProfileId(1L)).thenReturn(1L);
        when(certificationRepository.countByProfileId(1L)).thenReturn(0L);
        when(workExperienceRepository.sumMonthsByProfileId(1L)).thenReturn(6L);
        when(profileReviewRepository.countByProfileId(1L)).thenReturn(0L);
        when(profileReviewRepository.avgRatingByProfileId(1L)).thenReturn(0.0);

        when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RuntimeException("ML service down"));

        MlServiceClient.TrustScoreResult result =
                mlServiceClient.predictTrustScore(profile, false, false);

        assertThat(result).isNotNull();
        assertThat(result.level()).isEqualTo("UNKNOWN");
        assertThat(result.confidence()).isEqualTo(0.0);
    }
}
