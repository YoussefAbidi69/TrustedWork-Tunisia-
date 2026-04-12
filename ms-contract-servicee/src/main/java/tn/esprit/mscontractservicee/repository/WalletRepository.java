package tn.esprit.mscontractservicee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.entity.Wallet;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserCin(Long userCin);
    Optional<Wallet> findByStripeAccountId(String stripeAccountId);
    boolean existsByUserCin(Long userCin);
}
