package tn.esprit.smartjobboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.smartjobboard.dto.*;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.entity.JobOfferStatus;
import tn.esprit.smartjobboard.service.JobApplicationService;
import tn.esprit.smartjobboard.service.JobOfferService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobOfferController")
class JobOfferControllerTest {

    @Mock private JobOfferService jobOfferService;
    @Mock private JobApplicationService jobApplicationService;
    @InjectMocks private JobOfferController controller;

    private JobOfferResponse buildJobResponse(Long id, String title) {
        return JobOfferResponse.builder()
                .id(id)
                .clientId(10L)
                .title(title)
                .status(JobOfferStatus.DRAFT)
                .build();
    }

    @Test
    @DisplayName("create should return 201 with created job")
    void create() {
        JobOfferResponse resp = buildJobResponse(1L, "Dev");
        when(jobOfferService.create(any())).thenReturn(resp);

        JobOfferCreateRequest req = new JobOfferCreateRequest();
        req.setTitle("Dev");
        req.setDescription("Desc");
        req.setCategory("IT");
        req.setBudgetMin(BigDecimal.ZERO);
        req.setBudgetMax(BigDecimal.TEN);

        ResponseEntity<JobOfferResponse> result = controller.create(req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getTitle()).isEqualTo("Dev");
    }

    @Test
    @DisplayName("get should return 200 with job")
    void get() {
        JobOfferResponse resp = buildJobResponse(1L, "Dev");
        when(jobOfferService.get(1L)).thenReturn(resp);

        ResponseEntity<JobOfferResponse> result = controller.get(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("delete should return 204 no content")
    void delete() {
        ResponseEntity<Void> result = controller.delete(1L);

        verify(jobOfferService).delete(1L);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("publish should delegate and return 200")
    void publish() {
        JobOfferResponse resp = buildJobResponse(1L, "Dev");
        resp.setStatus(JobOfferStatus.PUBLISHED);
        when(jobOfferService.publish(1L)).thenReturn(resp);

        ResponseEntity<JobOfferResponse> result = controller.publish(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(JobOfferStatus.PUBLISHED);
    }

    @Test
    @DisplayName("close should delegate and return 200")
    void close() {
        JobOfferResponse resp = buildJobResponse(1L, "Dev");
        resp.setStatus(JobOfferStatus.CLOSED);
        when(jobOfferService.close(1L)).thenReturn(resp);

        ResponseEntity<JobOfferResponse> result = controller.close(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(JobOfferStatus.CLOSED);
    }

    @Test
    @DisplayName("list should return public feed when no filters")
    void listNoFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<JobOfferResponse> page = new PageImpl<>(List.of(buildJobResponse(1L, "Dev")));
        when(jobOfferService.publicFeed(any())).thenReturn(page);

        ResponseEntity<Page<JobOfferResponse>> result = controller.list(null, null, null, null, null, null, null, pageable);

        assertThat(result.getBody().getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("list should call search when filters provided")
    void listWithFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<JobOfferResponse> page = new PageImpl<>(List.of());
        when(jobOfferService.search(anyString(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<Page<JobOfferResponse>> result = controller.list("IT", null, null, null, null, null, null, pageable);

        verify(jobOfferService).search(eq("IT"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("myJobs should delegate to search with mine=true")
    void myJobs() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<JobOfferResponse> page = new PageImpl<>(List.of());
        when(jobOfferService.search(any(), any(), any(), any(), any(), any(), eq(true), any())).thenReturn(page);

        ResponseEntity<Page<JobOfferResponse>> result = controller.myJobs(pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("applications should return list for job")
    void applications() {
        when(jobApplicationService.listForJob(1L)).thenReturn(List.of());

        ResponseEntity<List<JobApplicationResponse>> result = controller.applications(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("apply should return 201")
    void apply() {
        JobApplicationResponse resp = JobApplicationResponse.builder()
                .id(1L).status(ApplicationStatus.PENDING).build();
        when(jobApplicationService.submit(any())).thenReturn(resp);

        ApplicationCreateRequest req = new ApplicationCreateRequest();
        ResponseEntity<JobApplicationResponse> result = controller.apply(5L, req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Verify jobOfferId was set from path variable
        verify(jobApplicationService).submit(argThat(r -> r.getJobOfferId().equals(5L)));
    }

    @Test
    @DisplayName("matches should delegate to service")
    void matches() {
        when(jobOfferService.matchesForJob(1L)).thenReturn(List.of());

        ResponseEntity<List<MatchFreelancerRowDto>> result = controller.matches(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("match should delegate to service")
    void match() {
        when(jobOfferService.matchesForJob(1L)).thenReturn(List.of());

        ResponseEntity<List<MatchFreelancerRowDto>> result = controller.match(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("previewSkills should delegate to service")
    void previewSkills() {
        PreviewSkillsResponse resp = new PreviewSkillsResponse(List.of("Java"));
        when(jobOfferService.previewSkills(any())).thenReturn(resp);

        PreviewSkillsRequest req = new PreviewSkillsRequest();
        ResponseEntity<PreviewSkillsResponse> result = controller.previewSkills(req);

        assertThat(result.getBody().getSkills()).contains("Java");
    }

    @Test
    @DisplayName("extractedSkills should return 200 with skills")
    void extractedSkills() {
        JobOfferResponse resp = buildJobResponse(1L, "Dev");
        resp.setExtractedSkills(List.of("Java"));
        when(jobOfferService.get(1L)).thenReturn(resp);

        ResponseEntity<PreviewSkillsResponse> result = controller.extractedSkills(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getSkills()).contains("Java");
    }

    @Test
    @DisplayName("successPrediction should delegate to service")
    void successPrediction() {
        SuccessPredictionViewDto dto = SuccessPredictionViewDto.builder().probability(0.85).build();
        when(jobOfferService.getSuccessPrediction(1L, 5L)).thenReturn(dto);

        ResponseEntity<SuccessPredictionViewDto> result = controller.successPrediction(1L, 5L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getProbability()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("update should delegate and return 200")
    void update() {
        JobOfferResponse resp = buildJobResponse(1L, "Updated Dev");
        when(jobOfferService.update(eq(1L), any())).thenReturn(resp);

        JobOfferUpdateRequest req = new JobOfferUpdateRequest();
        req.setTitle("Updated Dev");

        ResponseEntity<JobOfferResponse> result = controller.update(1L, req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getTitle()).isEqualTo("Updated Dev");
    }
}
