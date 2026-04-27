package tn.esprit.mscontractservicee.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;

import java.util.List;

@Repository
public interface SignatureSignerRepository extends JpaRepository<SignatureSigner, Long> {
    List<SignatureSigner> findBySignatureRequestId(Long signatureRequestId);
    long countBySignatureRequestIdAndStatus(Long signatureRequestId, SignatureSignerStatus status);
}

