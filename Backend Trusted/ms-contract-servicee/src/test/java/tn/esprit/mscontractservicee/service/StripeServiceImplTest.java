package tn.esprit.mscontractservicee.service;

import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StripeServiceImplTest {

    @InjectMocks
    private StripeServiceImpl stripeService;

    @Test
    void testCreatePaymentIntent_Simulated() throws Exception {
        ReflectionTestUtils.setField(stripeService, "simulationEnabled", true);
        BigDecimal amount = new BigDecimal("100.50");
        PaymentIntent pi = stripeService.createPaymentIntent(1L, amount, "usd", "test@mail.com");
        assertNotNull(pi);
        assertEquals("succeeded", pi.getStatus());
        assertTrue(pi.getId().startsWith("sim_"));
    }

    @Test
    void testGetPaymentIntent_Simulated() throws Exception {
        ReflectionTestUtils.setField(stripeService, "simulationEnabled", true);
        PaymentIntent pi = stripeService.getPaymentIntent("sim_123");
        assertNotNull(pi);
        assertEquals("succeeded", pi.getStatus());
    }

    @Test
    void testCreatePaymentIntent_ThrowsUnsupportedWhenSimulationDisabled() {
        ReflectionTestUtils.setField(stripeService, "simulationEnabled", false);
        assertThrows(UnsupportedOperationException.class, () -> 
            stripeService.createPaymentIntent(1L, BigDecimal.ONE, "usd", "a@b.com"));
    }

    @Test
    void testGetPaymentIntent_ThrowsUnsupportedWhenSimulationDisabled() {
        ReflectionTestUtils.setField(stripeService, "simulationEnabled", false);
        assertThrows(UnsupportedOperationException.class, () -> 
            stripeService.getPaymentIntent("sim_123"));
    }
}
