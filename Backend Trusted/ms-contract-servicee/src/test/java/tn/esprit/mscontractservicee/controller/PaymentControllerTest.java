package tn.esprit.mscontractservicee.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tn.esprit.mscontractservicee.dto.PaymentIntentResponse;
import tn.esprit.mscontractservicee.service.IPaymentService;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private IPaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void testCreatePaymentIntent_Success() {
        PaymentIntentResponse response = PaymentIntentResponse.builder()
                .clientSecret("pi_secret_test")
                .paymentIntentId("pi_123")
                .build();

        when(paymentService.createPaymentIntent(1L, "client@test.com")).thenReturn(response);

        ResponseEntity<?> result = paymentController.createPaymentIntent(1L, "client@test.com");

        assertEquals(200, result.getStatusCode().value());
        PaymentIntentResponse body = (PaymentIntentResponse) result.getBody();
        assertNotNull(body);
        assertEquals("pi_secret_test", body.getClientSecret());
        assertEquals("pi_123", body.getPaymentIntentId());
    }

    @Test
    void testCreatePaymentIntent_Error() {
        when(paymentService.createPaymentIntent(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Contract not found"));

        ResponseEntity<?> result = paymentController.createPaymentIntent(999L, "client@test.com");

        assertEquals(400, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Contract not found", body.get("error"));
    }

    @Test
    void testConfirmPayment_Success() {
        doNothing().when(paymentService).confirmPayment("pi_123", 1L);

        ResponseEntity<?> result = paymentController.confirmPayment("pi_123", 1L);

        assertEquals(200, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals(true, body.get("success"));
    }

    @Test
    void testGetPaymentStatus_Success() {
        when(paymentService.getPaymentStatus("pi_123")).thenReturn("succeeded");

        ResponseEntity<?> result = paymentController.getPaymentStatus("pi_123");

        assertEquals(200, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("succeeded", body.get("status"));
    }

    @Test
    void testReleasePayment_WithAmount() {
        doNothing().when(paymentService).releasePaymentToFreelancer(anyLong(), anyLong(), any());

        ResponseEntity<?> result = paymentController.releasePayment(1L, 2L, new BigDecimal("500"));

        assertEquals(200, result.getStatusCode().value());
    }

    @Test
    void testReleasePayment_WithoutAmount_CallsApproved() {
        doNothing().when(paymentService).releaseApprovedMilestone(2L);

        ResponseEntity<?> result = paymentController.releasePayment(1L, 2L, null);

        assertEquals(200, result.getStatusCode().value());
        verify(paymentService, times(1)).releaseApprovedMilestone(2L);
    }

    @Test
    void testConfirmPayment_Error_NullMessageUsesUnknownError() {
        doThrow(new RuntimeException()).when(paymentService).confirmPayment(anyString(), anyLong());

        ResponseEntity<?> result = paymentController.confirmPayment("pi_123", 1L);

        assertEquals(400, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Unknown error", body.get("error"));
    }

    @Test
    void testReleasePayment_Error_NullMessageUsesUnknownError() {
        doThrow(new RuntimeException()).when(paymentService).releaseApprovedMilestone(anyLong());

        ResponseEntity<?> result = paymentController.releasePayment(1L, 2L, null);

        assertEquals(400, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Unknown error", body.get("error"));
    }

    @Test
    void testGetPaymentStatus_Error_NullMessageUsesUnknownError() {
        when(paymentService.getPaymentStatus(anyString())).thenThrow(new RuntimeException());

        ResponseEntity<?> result = paymentController.getPaymentStatus("pi_123");

        assertEquals(400, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Unknown error", body.get("error"));
    }
    @Test
    void testCreatePaymentIntent_Error_WithMessage() {
        when(paymentService.createPaymentIntent(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Specific error"));
        ResponseEntity<?> result = paymentController.createPaymentIntent(1L, "x@x.com");
        assertEquals(400, result.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("Specific error", body.get("error"));
    }

    @Test
    void testConfirmPayment_Error_WithMessage() {
        doThrow(new RuntimeException("Confirm failed")).when(paymentService).confirmPayment(anyString(), anyLong());
        ResponseEntity<?> result = paymentController.confirmPayment("pi_x", 1L);
        assertEquals(400, result.getStatusCode().value());
        assertEquals("Confirm failed", ((Map<?, ?>) result.getBody()).get("error"));
    }

    @Test
    void testReleasePayment_Error_WithMessage() {
        doThrow(new RuntimeException("Release failed")).when(paymentService).releaseApprovedMilestone(anyLong());
        ResponseEntity<?> result = paymentController.releasePayment(1L, 2L, null);
        assertEquals(400, result.getStatusCode().value());
        assertEquals("Release failed", ((Map<?, ?>) result.getBody()).get("error"));
    }

    @Test
    void testReleasePayment_WithAmount_Error_WithMessage() {
        doThrow(new RuntimeException("Amount error")).when(paymentService)
                .releasePaymentToFreelancer(anyLong(), anyLong(), any());
        ResponseEntity<?> result = paymentController.releasePayment(1L, 2L, new BigDecimal("100"));
        assertEquals(400, result.getStatusCode().value());
        assertEquals("Amount error", ((Map<?, ?>) result.getBody()).get("error"));
    }

    @Test
    void testGetPaymentStatus_Error_WithMessage() {
        when(paymentService.getPaymentStatus(anyString())).thenThrow(new RuntimeException("Status error"));
        ResponseEntity<?> result = paymentController.getPaymentStatus("pi_x");
        assertEquals(400, result.getStatusCode().value());
        assertEquals("Status error", ((Map<?, ?>) result.getBody()).get("error"));
    }
}
