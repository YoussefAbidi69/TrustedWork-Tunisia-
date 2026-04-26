package tn.esprit.mscontractservicee.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mscontractservicee.service.IPaymentService;

import java.math.BigDecimal;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;  //  Interface

    private static final String ERROR_KEY = "error";
    private static final String UNKNOWN_ERROR = "Unknown error";

    @PostMapping("/create-intent")
    public ResponseEntity<Object> createPaymentIntent(
            @RequestParam Long contractId,
            @RequestParam String email) {
        try {
            var response = paymentService.createPaymentIntent(contractId, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : UNKNOWN_ERROR));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<Object> confirmPayment(
            @RequestParam String paymentIntentId,
            @RequestParam Long contractId) {
        try {
            paymentService.confirmPayment(paymentIntentId, contractId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : UNKNOWN_ERROR));
        }
    }

    @PostMapping("/release")
    public ResponseEntity<Object> releasePayment(
            @RequestParam Long contractId,
            @RequestParam Long milestoneId,
            @RequestParam(required = false) BigDecimal amount) {
        try {
            if (amount == null) {
                paymentService.releaseApprovedMilestone(milestoneId);
            } else {
                paymentService.releasePaymentToFreelancer(contractId, milestoneId, amount);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : UNKNOWN_ERROR));
        }
    }

    @GetMapping("/status/{paymentIntentId}")
    public ResponseEntity<Object> getPaymentStatus(@PathVariable String paymentIntentId) {
        try {
            String status = paymentService.getPaymentStatus(paymentIntentId);
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage() != null ? e.getMessage() : UNKNOWN_ERROR));
        }
    }
}
