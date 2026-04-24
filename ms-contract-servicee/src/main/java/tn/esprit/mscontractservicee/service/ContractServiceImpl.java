package tn.esprit.mscontractservicee.service;

import tn.esprit.mscontractservicee.dto.ContractWalletIdsResponse;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Wallet;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.feign.UserServiceClient;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractServiceImpl implements IContractService {

    @Value("${contract.creation.requireFreelancerKycApproved:true}")
    private boolean requireFreelancerKycApproved;

    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserServiceClient userServiceClient;
    private final IWalletService walletService;
    private final ContractTotalService contractTotalService;

    @Override
    public Contract createContract(Contract contract, Long authenticatedCin) {
        Long freelancerCin = contract.getFreelancerCin();
        if (freelancerCin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerCin is required");
        }

        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user cin is missing");
        }

        // Store CINs inside contract.user identifiers.
        contract.setClientCin(authenticatedCin);
        contract.setClientWalletCin(null);
        contract.setFreelancerWalletCin(null);

        log.info("Creating new contract for authenticated client: {} and freelancer: {}",
                authenticatedCin, freelancerCin);

        // Verify that the freelancer exists
        UserDTO freelancer = fetchUserByCin(freelancerCin);
        if (!"FREELANCER".equals(freelancer.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User with cin " + freelancerCin + " is not a FREELANCER");
        }

        // Verify KYC
        if (requireFreelancerKycApproved
                && (freelancer.getKycStatus() == null || !freelancer.getKycStatus().equals("APPROVED"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Freelancer KYC not approved. Current status: " + freelancer.getKycStatus());
        }

        // Link wallets to the contract
        linkWallets(contract);

        // Create the contract
        contract.setReference("CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        if (contract.getVersion() == null || contract.getVersion() < 1) {
            contract.setVersion(1);
        }
        contract.setStatus(ContractStatus.DRAFT);

        Contract savedContract = contractRepository.save(contract);
        log.info("Contract created successfully with reference: {}", savedContract.getReference());

        return savedContract;
    }

    @Override
    public Contract updateContract(Long id, Contract contract) {
        log.info("Updating contract with id: {}", id);
        Contract existing = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));

        if (existing.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract cannot be updated. Status: " + existing.getStatus());
        }

        existing.setProjectTitle(contract.getProjectTitle());
        existing.setDescription(contract.getDescription());
        BigDecimal newBudget = contract.getMontantTotal() != null ? contract.getMontantTotal() : existing.getMontantTotal();
        if (newBudget != null && newBudget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montantTotal (budget) must be > 0");
        }

        long milestoneCount = milestoneRepository.countByContractId(id);
        if (milestoneCount > 0 && newBudget != null) {
            BigDecimal allocated = contractTotalService.computeMilestonesTotal(id);
            if (allocated.compareTo(newBudget) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot set contract budget below milestones total. budget=" + newBudget + " milestonesTotal=" + allocated);
            }
        }
        existing.setMontantTotal(newBudget);
        existing.setSlaFreelancerHeures(contract.getSlaFreelancerHeures());
        existing.setSlaClientJours(contract.getSlaClientJours());
        existing.setDateDebut(contract.getDateDebut());
        existing.setDateFin(contract.getDateFin());
        existing.setCommissionRate(contract.getCommissionRate());
        existing.setUpdatedAt(LocalDateTime.now());

        return contractRepository.save(existing);
    }

    @Override
    public Optional<Contract> findById(Long id) {
        return contractRepository.findById(id);
    }

    @Override
    public Page<Contract> findAll(Pageable pageable) {
        return contractRepository.findAll(pageable);
    }

    @Override
    public Page<Contract> findByUserCin(Long userCin, Pageable pageable) {
        return contractRepository.findByClientCinOrFreelancerCin(userCin, userCin, pageable);
    }

    @Override
    public Page<Contract> findByClientCin(Long clientCin, Pageable pageable) {
        return contractRepository.findByClientCin(clientCin, pageable);
    }

    @Override
    public Page<Contract> findSignedByFreelancerCin(Long freelancerCin, Pageable pageable) {
        return contractRepository.findByFreelancerCinAndDateSignatureIsNotNull(freelancerCin, pageable);
    }

    @Override
    public Contract updateStatus(Long id, ContractStatus status) {
        log.info("Updating contract {} status to: {}", id, status);
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));

        contract.setStatus(status);
        contract.setUpdatedAt(LocalDateTime.now());

        if (status == ContractStatus.CANCELLED) {
            contract.setCancelledAt(LocalDateTime.now());
        }

        return contractRepository.save(contract);
    }

    @Override
    public void deleteContract(Long id) {
        log.info("Deleting contract with id: {}", id);
        Contract existing = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        if (existing.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT contracts can be deleted. Current status: " + existing.getStatus());
        }
        contractRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return contractRepository.existsById(id);
    }

    // ==================== UTILITY METHODS ====================

    @Override
    public UserDTO getClientInfo(Long contractId) {
        log.info("Getting client info for contract: {}", contractId);

        // Verify that the contract exists
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));

        return fetchUserByCin(contract.getClientCin());
    }

    @Override
    public UserDTO getFreelancerInfo(Long contractId) {
        log.info("Getting freelancer info for contract: {}", contractId);

        // Verify that the contract exists
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));

        return fetchUserByCin(contract.getFreelancerCin());
    }

    @Override
    public ContractWalletIdsResponse getWalletIds(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + contractId));

        if (contract.getClientWalletCin() == null || contract.getFreelancerWalletCin() == null) {
            linkWallets(contract);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);
        }

        return ContractWalletIdsResponse.builder()
                .contractId(contract.getId())
                .clientCin(contract.getClientCin())
                .clientWalletCin(contract.getClientWalletCin())
                .freelancerCin(contract.getFreelancerCin())
                .freelancerWalletCin(contract.getFreelancerWalletCin())
                .build();
    }

    @Override
    public Contract finalizeForSignature(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract cannot be finalized. Status: " + contract.getStatus());
        }

        long milestoneCount = milestoneRepository.countByContractId(contractId);
        if (milestoneCount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot finalize a contract without milestones");
        }

        if (contract.getMontantTotal() == null || contract.getMontantTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "montantTotal (budget) is required before finalize");
        }
        BigDecimal computed;
        try {
            computed = contractTotalService.computeMilestonesTotal(contractId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        if (computed.compareTo(contract.getMontantTotal()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestones total must equal contract budget before finalize. budget=" + contract.getMontantTotal()
                            + " milestonesTotal=" + computed);
        }

        if (contract.getVersion() == null || contract.getVersion() < 1) {
            contract.setVersion(1);
        }

        contract.setStatus(ContractStatus.PENDING_SIGNATURE);
        contract.setFinalizedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        return contractRepository.save(contract);
    }

    private void linkWallets(Contract contract) {
        if (contract.getClientCin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientCin is required to link wallets");
        }
        if (contract.getFreelancerCin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "freelancerCin is required to link wallets");
        }

        // Ensure wallets exist; contract stores CINs (not wallet primary keys).
        walletService.getOrCreateWallet(contract.getClientCin());
        walletService.getOrCreateWallet(contract.getFreelancerCin());
        contract.setClientWalletCin(contract.getClientCin());
        contract.setFreelancerWalletCin(contract.getFreelancerCin());
    }

    private UserDTO fetchUserByCin(Long cin) {
        try {
            // Prefer the direct user lookup when available.
            return userServiceClient.getUserByCin(cin);
        } catch (FeignException e) {
            log.warn("User lookup failed (primary) cin={}, status={}, method={}, url={}, msg={}, body={}",
                    cin,
                    e.status(),
                    (e.request() != null ? e.request().httpMethod() : null),
                    (e.request() != null ? e.request().url() : null),
                    e.getMessage(),
                    safeFeignBody(e));
            // Newer ms-user versions do not expose GET /users/{cin}. Fallback to /kyc/status/{cin}.
            // Note: when ms-user has PUT /users/{cin} but no GET /users/{cin}, calling GET can return 405.
            // Some versions can also return 5xx for /users/{cin} due to internal errors; try the KYC endpoint anyway.
            if (e.status() == 404 || e.status() == 405 || e.status() >= 500) {
                try {
                    return userServiceClient.getUserByCinFromKycStatus(cin);
                } catch (FeignException e2) {
                    log.warn("User lookup failed (fallback kyc/status) cin={}, status={}, method={}, url={}, msg={}, body={}",
                            cin,
                            e2.status(),
                            (e2.request() != null ? e2.request().httpMethod() : null),
                            (e2.request() != null ? e2.request().url() : null),
                            e2.getMessage(),
                            safeFeignBody(e2));
                    if (e2.status() == 404) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with cin: " + cin, e2);
                    }
                    if (e2.status() == 401 || e2.status() == 403) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token", e2);
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service error", e2);
                }
            }
            if (e.status() == 401 || e.status() == 403) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service error", e);
        }
    }

    private static String safeFeignBody(FeignException e) {
        try {
            String body = e.contentUTF8();
            if (body == null) return null;
            // Avoid logging huge payloads.
            if (body.length() > 2000) {
                return body.substring(0, 2000) + "...(truncated)";
            }
            return body;
        } catch (Exception ignored) {
            return null;
        }
    }
}
