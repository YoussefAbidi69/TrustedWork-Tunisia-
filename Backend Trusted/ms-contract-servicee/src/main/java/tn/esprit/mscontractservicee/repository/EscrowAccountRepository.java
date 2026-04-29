package tn.esprit.mscontractservicee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.entity.EscrowAccount;
import java.util.Optional;

@Repository
public interface EscrowAccountRepository extends JpaRepository<EscrowAccount, Long> {
    Optional<EscrowAccount> findByContractId(Long contractId);  // ✅ Vérifier que c'est "Id" pas "ld"
}