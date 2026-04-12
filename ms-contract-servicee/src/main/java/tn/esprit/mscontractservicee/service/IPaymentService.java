package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.PaymentIntentResponse;
import java.math.BigDecimal;

public interface IPaymentService {

    PaymentIntentResponse createPaymentIntent(Long contractId, String email) throws Exception;

    void confirmPayment(String paymentIntentId, Long contractId) throws Exception;

    /**
     * Releases the payment for a milestone that has been validated (APPROVED/AUTO_APPROVED).
     * Amount is derived from the milestone itself (no caller-provided amount).
     */
    void releaseApprovedMilestone(Long milestoneId) throws Exception;

    /**
     * Legacy/manual release API. Amount is validated against the milestone amount.
     * Prefer {@link #releaseApprovedMilestone(Long)} which is amount-safe.
     */
    void releasePaymentToFreelancer(Long contractId, Long milestoneId, BigDecimal amount) throws Exception;

    String getPaymentStatus(String paymentIntentId) throws Exception;

}
