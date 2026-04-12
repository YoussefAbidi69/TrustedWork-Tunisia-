package tn.esprit.mscontractservicee.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.mscontractservicee.service.IPaymentService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;  //  Interface

    @PostMapping("/create-intent")
    public ResponseEntity<?> createPaymentIntent(
            @RequestParam Long contractId,
            @RequestParam String email) {
        try {
            var response = paymentService.createPaymentIntent(contractId, email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @RequestParam String paymentIntentId,
            @RequestParam Long contractId) {
        try {
            paymentService.confirmPayment(paymentIntentId, contractId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/release")
    public ResponseEntity<?> releasePayment(
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{paymentIntentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentIntentId) {
        try {
            String status = paymentService.getPaymentStatus(paymentIntentId);
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
