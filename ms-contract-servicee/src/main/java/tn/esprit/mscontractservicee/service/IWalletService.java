package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.entity.Wallet;
import java.math.BigDecimal;

public interface IWalletService {

    Wallet getOrCreateWallet(Long userCin);

    Wallet createWallet(Long userCin);

    Wallet credit(Long userCin, BigDecimal amount, String description);

    Wallet debit(Long userCin, BigDecimal amount, String description);

    String createStripeAccount(Long userCin, String email, String country) throws Exception;

    String getStripeAccountStatus(Long userCin) throws Exception;

    String getOnboardingLink(Long userCin) throws Exception;
}
