package tn.esprit.smartjobboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.smartjobboard.dto.ApplicationCreateRequest;
import tn.esprit.smartjobboard.dto.ApplicationStatusUpdateRequest;
import tn.esprit.smartjobboard.dto.JobApplicationResponse;
import tn.esprit.smartjobboard.entity.ApplicationStatus;
import tn.esprit.smartjobboard.service.JobApplicationService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobApplicationController")
class JobApplicationControllerTest {

    @Mock private JobApplicationService jobApplicationService;
    @InjectMocks private JobApplicationController controller;

    private JobApplicationResponse buildResponse(Long id, ApplicationStatus status) {
        return JobApplicationResponse.builder()
                .id(id)
                .jobOfferId(1L)
                .freelancerId(5L)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("submit should delegate to service and return 201 CREATED")
    void submit() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.PENDING);
        when(jobApplicationService.submit(any())).thenReturn(resp);

        ApplicationCreateRequest req = new ApplicationCreateRequest();
        req.setJobOfferId(1L);
        req.setCoverLetter("Cover letter");
        req.setProposedRate(BigDecimal.valueOf(1000));

        ResponseEntity<JobApplicationResponse> result = controller.submit(req);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(jobApplicationService).submit(req);
    }

    @Test
    @DisplayName("myApplications should return 200 with list")
    void myApplications() {
        when(jobApplicationService.listMineForFreelancer()).thenReturn(List.of(buildResponse(1L, ApplicationStatus.PENDING)));

        ResponseEntity<List<JobApplicationResponse>> result = controller.myApplications();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("updateStatus should delegate and return 200")
    void updateStatus() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.SHORTLISTED);
        when(jobApplicationService.updateStatus(eq(1L), any())).thenReturn(resp);

        ApplicationStatusUpdateRequest req = new ApplicationStatusUpdateRequest();
        req.setStatus(ApplicationStatus.SHORTLISTED);

        ResponseEntity<JobApplicationResponse> result = controller.updateStatus(1L, req);

        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
    }

    @Test
    @DisplayName("withdraw should delegate and return 200")
    void withdraw() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.WITHDRAWN);
        when(jobApplicationService.withdraw(1L)).thenReturn(resp);

        ResponseEntity<JobApplicationResponse> result = controller.withdraw(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("withdrawPatch should delegate and return 200")
    void withdrawPatch() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.WITHDRAWN);
        when(jobApplicationService.withdraw(1L)).thenReturn(resp);

        ResponseEntity<JobApplicationResponse> result = controller.withdrawPatch(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("shortlist should create SHORTLISTED request and delegate")
    void shortlist() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.SHORTLISTED);
        when(jobApplicationService.updateStatus(eq(1L), any())).thenReturn(resp);

        ResponseEntity<JobApplicationResponse> result = controller.shortlist(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        verify(jobApplicationService).updateStatus(eq(1L), argThat(r -> r.getStatus() == ApplicationStatus.SHORTLISTED));
    }

    @Test
    @DisplayName("accept should create ACCEPTED request and delegate")
    void accept() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.ACCEPTED);
        when(jobApplicationService.updateStatus(eq(1L), any())).thenReturn(resp);

        ResponseEntity<JobApplicationResponse> result = controller.accept(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
    }

    @Test
    @DisplayName("reject should create REJECTED request and delegate")
    void reject() {
        JobApplicationResponse resp = buildResponse(1L, ApplicationStatus.REJECTED);
        when(jobApplicationService.updateStatus(eq(1L), any())).thenReturn(resp);

        ResponseEntity<JobApplicationResponse> result = controller.reject(1L);

        assertThat(result.getBody().getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }
}
