package tn.esprit.mscontractservicee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private IStripeService stripeService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Wallet buildWallet(Long cin, BigDecimal balance) {
        return Wallet.builder()
                .id(1L)
                .userCin(cin)
                .balance(balance)
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .totalCommissionPaid(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testGetOrCreateWallet_Existing() {
        Wallet wallet = buildWallet(123L, new BigDecimal("500"));
        when(walletRepository.findByUserCin(123L)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getOrCreateWallet(123L);

        assertNotNull(result);
        assertEquals(123L, result.getUserCin());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateWallet_Creates() {
        Wallet newWallet = buildWallet(456L, BigDecimal.ZERO);
        when(walletRepository.findByUserCin(456L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        Wallet result = walletService.getOrCreateWallet(456L);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void testCredit_AddsToBalance() {
        Wallet wallet = buildWallet(123L, new BigDecimal("100"));
        when(walletRepository.findByUserCin(123L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        Wallet result = walletService.credit(123L, new BigDecimal("200"), "test credit");

        assertEquals(new BigDecimal("300"), result.getBalance());
        assertEquals(new BigDecimal("200"), result.getTotalEarned());
    }

    @Test
    void testDebit_SubtractsFromBalance() {
        Wallet wallet = buildWallet(123L, new BigDecimal("500"));
        when(walletRepository.findByUserCin(123L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        Wallet result = walletService.debit(123L, new BigDecimal("200"), "test debit");

        assertEquals(new BigDecimal("300"), result.getBalance());
        assertEquals(new BigDecimal("200"), result.getTotalSpent());
    }

    @Test
    void testDebit_InsufficientBalance_ThrowsException() {
        Wallet wallet = buildWallet(123L, new BigDecimal("50"));
        when(walletRepository.findByUserCin(123L)).thenReturn(Optional.of(wallet));

        BigDecimal amount = new BigDecimal("100");
        assertThrows(RuntimeException.class, () -> walletService.debit(123L, amount, "overdraft attempt"));
    }

    @Test
    void testCreateWallet() {
        Wallet newWallet = buildWallet(789L, BigDecimal.ZERO);
        when(walletRepository.save(any(Wallet.class))).thenReturn(newWallet);

        Wallet result = walletService.createWallet(789L);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void testStripeSimulation_Operations() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(walletService, "simulationEnabled", true);
        when(walletRepository.findByUserCin(123L)).thenReturn(Optional.of(buildWallet(123L, BigDecimal.ZERO)));

        String accountId = walletService.createStripeAccount(123L, "t@t.com", "TN");
        assertTrue(accountId.startsWith("acct_sim_"));

        String status = walletService.getStripeAccountStatus(123L);
        assertEquals("ACTIVE", status);

        String link = walletService.getOnboardingLink(123L);
        assertTrue(link.contains("simulation-success"));
    }

    @Test
    void testStripeSimulation_Disabled_Throws() {
        org.springframework.test.util.ReflectionTestUtils.setField(walletService, "simulationEnabled", false);
        assertThrows(UnsupportedOperationException.class, () -> walletService.createStripeAccount(1L, "a@b.com", "TN"));
        assertThrows(UnsupportedOperationException.class, () -> walletService.getStripeAccountStatus(1L));
        assertThrows(UnsupportedOperationException.class, () -> walletService.getOnboardingLink(1L));
    }
}
