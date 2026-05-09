package tn.esprit.mscontractservicee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.mscontractservicee.dto.dispute.DisputeAiRecommendation;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DisputeRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeAiServiceImplTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private RestTemplate restTemplate;

    private DisputeAiServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisputeAiServiceImpl(disputeRepository, contractRepository, milestoneRepository, restTemplate, new ObjectMapper());
    }

    @Test
    void testAnalyze_NullDisputeId_Throws() {
        assertThrows(ResponseStatusException.class, () -> service.analyze(null, 1L));
    }

    @Test
    void testAnalyze_NoApiKey_UsesFallbackAndDoesNotCallGemini() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertNotNull(res);
        assertEquals(1L, res.getDisputeId());
        assertTrue(res.isFallback());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void testAnalyze_WithApiKey_CallsGeminiAndParses() throws Exception {
        ReflectionTestUtils.setField(service, "geminiApiKey", "AIza_test_key");

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        String candidateText = "{\"suggestedDecision\":\"SPLIT\",\"confidenceScore\":0.7,\"riskLevel\":\"LOW\",\"summary\":\"s\",\"reasoning\":\"r\",\"suggestedMontantRembourse\":10,\"suggestedMontantLibere\":20,\"keyFactors\":[\"a\",\"b\"]}";
        String responseBody = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "candidates", java.util.List.of(java.util.Map.of(
                        "content", java.util.Map.of(
                                "parts", java.util.List.of(java.util.Map.of("text", candidateText))
                        )
                ))
        ));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertNotNull(res);
        assertFalse(res.isFallback());
        assertEquals("LOW", res.getRiskLevel());
        assertEquals(new BigDecimal("10.0"), res.getSuggestedMontantRembourse());
        assertEquals(new BigDecimal("20.0"), res.getSuggestedMontantLibere());
    }

    @Test
    void testAnalyze_WithApiKey_Non2xxResponse_FallsBack() {
        ReflectionTestUtils.setField(service, "geminiApiKey", "AIza_test_key");

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"bad\"}"));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertNotNull(res);
        assertTrue(res.isFallback());
    }

    @Test
    void testAnalyze_WithApiKey_HttpClientError_FallsBack() {
        ReflectionTestUtils.setField(service, "geminiApiKey", "AIza_test_key");

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "too many"));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertNotNull(res);
        assertTrue(res.isFallback());
    }

    @Test
    void testAnalyze_Fallback_UsesMilestoneAmountAndResolvedFreelancerWhenDefensePresent() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setMilestoneId(5L);
        dispute.setPreuvesDefense("proof");
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setMontantTotal(new BigDecimal("999"));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        Milestone milestone = new Milestone();
        milestone.setId(5L);
        milestone.setMontant(new BigDecimal("100"));
        when(milestoneRepository.findById(5L)).thenReturn(Optional.of(milestone));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertTrue(res.isFallback());
        assertEquals("RESOLVED_FREELANCER", res.getSuggestedDecision());
        assertEquals(new BigDecimal("100"), res.getSuggestedMontantLibere());
    }

    @Test
    void testAnalyze_Fallback_UsesContractTotalWhenNoMilestone() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setMilestoneId(null);
        dispute.setPreuvesDefense("proof");
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setMontantTotal(new BigDecimal("123"));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertTrue(res.isFallback());
        assertEquals(new BigDecimal("123"), res.getSuggestedMontantLibere());
    }

    @Test
    void testAnalyze_GeminiResponseWithCodeFences_ParsesCorrectly() throws Exception {
        ReflectionTestUtils.setField(service, "geminiApiKey", "AIza_test");

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(new Contract()));

        String fenceJson = "```json\n{\"suggestedDecision\":\"SPLIT\"}\n```";
        String responseBody = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "candidates", java.util.List.of(java.util.Map.of(
                        "content", java.util.Map.of(
                                "parts", java.util.List.of(java.util.Map.of("text", fenceJson))
                        )
                ))
        ));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertEquals("SPLIT", res.getSuggestedDecision());
        assertFalse(res.isFallback());
    }

    @Test
    void testAnalyze_Fallback_BothPartiesHaveProof_Splits() {
        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setContractId(10L);
        dispute.setPreuvesPlaignant("p");
        dispute.setPreuvesDefense("d");
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        Contract contract = new Contract();
        contract.setId(10L);
        contract.setMontantTotal(new BigDecimal("100"));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));

        DisputeAiRecommendation res = service.analyze(1L, 999L);
        assertEquals("SPLIT", res.getSuggestedDecision());
        assertEquals(new BigDecimal("50.00"), res.getSuggestedMontantRembourse());
    }
}
