package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.PaymentIntentResponse;
import java.math.BigDecimal;

public interface IPaymentService {

    PaymentIntentResponse createPaymentIntent(Long contractId, String email);

    void confirmPayment(String paymentIntentId, Long contractId);

    /**
     * Releases the payment for a milestone that has been validated (APPROVED/AUTO_APPROVED).
     * Amount is derived from the milestone itself (no caller-provided amount).
     */
    void releaseApprovedMilestone(Long milestoneId);

    /**
     * Legacy/manual release API. Amount is validated against the milestone amount.
     * Prefer {@link #releaseApprovedMilestone(Long)} which is amount-safe.
     */
    void releasePaymentToFreelancer(Long contractId, Long milestoneId, BigDecimal amount);

    String getPaymentStatus(String paymentIntentId);

    /**
     * Refunds the amount of a milestone back to the client's wallet.
     * Used when a milestone is cancelled due to SLA breaches.
     */
    void refundMilestoneToClient(Long milestoneId);

}
