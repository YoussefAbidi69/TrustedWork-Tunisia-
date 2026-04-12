package tn.esprit.mscontractservicee.service;

import com.stripe.model.PaymentIntent;

import java.math.BigDecimal;

public interface IStripeService {

    PaymentIntent createPaymentIntent(Long contractId, BigDecimal amount, String currency, String email) throws Exception;

    PaymentIntent getPaymentIntent(String paymentIntentId) throws Exception;
}