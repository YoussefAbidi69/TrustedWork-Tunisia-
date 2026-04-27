package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.ContractWalletIdsResponse;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IContractService {

    Contract createContract(Contract contract, Long authenticatedCin);
    Contract updateContract(Long id, Contract contract);
    Optional<Contract> findById(Long id);
    Page<Contract> findAll(Pageable pageable);
    Page<Contract> findByUserCin(Long userCin, Pageable pageable);
    Page<Contract> findByClientCin(Long clientCin, Pageable pageable);
    Page<Contract> findSignedByFreelancerCin(Long freelancerCin, Pageable pageable);
    Contract updateStatus(Long id, ContractStatus status);
    void deleteContract(Long id);
    boolean existsById(Long id);

    // Signature flow
    Contract finalizeForSignature(Long contractId);

    // Contract-related user lookups
    UserDTO getClientInfo(Long contractId);
    UserDTO getFreelancerInfo(Long contractId);

    ContractWalletIdsResponse getWalletIds(Long contractId);
}

