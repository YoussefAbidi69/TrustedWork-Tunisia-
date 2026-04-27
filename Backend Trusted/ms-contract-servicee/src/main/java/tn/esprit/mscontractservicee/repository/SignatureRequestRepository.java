package tn.esprit.mscontractservicee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.entity.SignatureRequest;

import java.util.Optional;

@Repository
public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, Long> {
    Optional<SignatureRequest> findTopByContractIdOrderByIdDesc(Long contractId);
}

