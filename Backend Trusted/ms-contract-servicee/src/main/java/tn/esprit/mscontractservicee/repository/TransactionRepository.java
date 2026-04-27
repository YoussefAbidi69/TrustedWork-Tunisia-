package tn.esprit.mscontractservicee.repository;

import tn.esprit.mscontractservicee.entity.Transaction;
import tn.esprit.mscontractservicee.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByMilestoneIdAndType(Long milestoneId, TransactionType type);

    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
