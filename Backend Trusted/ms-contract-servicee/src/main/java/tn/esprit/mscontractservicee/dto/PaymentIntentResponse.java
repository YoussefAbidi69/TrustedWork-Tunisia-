package tn.esprit.mscontractservicee.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentIntentResponse {
    private String clientSecret;
    private String paymentIntentId;
}