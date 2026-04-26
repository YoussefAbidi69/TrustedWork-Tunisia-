package tn.esprit.mscontractservicee.service;

import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class StripeServiceImpl implements IStripeService {

    @Value("${payment.simulation.enabled:false}")
    private boolean simulationEnabled;

    private static final String SIMULATION_BANNER = "========================================";

    @Override
    public PaymentIntent createPaymentIntent(Long contractId, BigDecimal amount, String currency, String email) throws com.stripe.exception.StripeException {

        if (simulationEnabled) {
            log.info(SIMULATION_BANNER);
            log.info(" SIMULATION MODE ENABLED ");
            log.info(SIMULATION_BANNER);
            log.info("Creating SIMULATED payment intent for contract: {}", contractId);
            log.info("Amount: {} {}", amount, currency);
            log.info("Email: {}", email);
            log.info(SIMULATION_BANNER);

            // Créer un PaymentIntent simulé
            PaymentIntent mockIntent = new PaymentIntent();
            mockIntent.setId("sim_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            mockIntent.setClientSecret("sim_secret_" + UUID.randomUUID().toString().substring(0, 16).toUpperCase());
            mockIntent.setStatus("succeeded");
            mockIntent.setAmount(amount.longValue() * 100);
            mockIntent.setCurrency(currency != null ? currency : "usd");
            mockIntent.setDescription("SIMULATION: Contrat TrustedWork #" + contractId);

            log.info(" Simulated Payment Intent created:");
            log.info("   ID: {}", mockIntent.getId());
            log.info("   Client Secret: {}", mockIntent.getClientSecret());
            log.info("   Status: {}", mockIntent.getStatus());
            log.info(SIMULATION_BANNER);

            return mockIntent;
        }

        // Code réel Stripe (sera utilisé quand simulation.enabled = false)
        throw new UnsupportedOperationException("Stripe is not available. Please enable simulation mode or configure Stripe.");
    }

    @Override
    public PaymentIntent getPaymentIntent(String paymentIntentId) throws com.stripe.exception.StripeException {

        if (simulationEnabled) {
            log.info(" SIMULATION: Getting payment intent: {}", paymentIntentId);

            // Vérifier si c'est un ID simulé
            if (paymentIntentId.startsWith("sim_")) {
                PaymentIntent mockIntent = new PaymentIntent();
                mockIntent.setId(paymentIntentId);
                mockIntent.setStatus("succeeded");
                mockIntent.setClientSecret("sim_secret_mock");
                log.info(" Simulated payment found - Status: succeeded");
                return mockIntent;
            }
        }

        throw new UnsupportedOperationException("Stripe is not available");
    }
}
