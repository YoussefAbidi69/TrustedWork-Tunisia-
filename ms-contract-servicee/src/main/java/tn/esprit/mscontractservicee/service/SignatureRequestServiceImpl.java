package tn.esprit.mscontractservicee.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.UserDTO;
import tn.esprit.mscontractservicee.dto.signing.ContractSnapshot;
import tn.esprit.mscontractservicee.dto.signing.MilestoneSnapshot;
import tn.esprit.mscontractservicee.dto.signing.SignatureRequestCreateResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningRequestViewResponse;
import tn.esprit.mscontractservicee.dto.signing.SigningSignRequest;
import tn.esprit.mscontractservicee.dto.signing.SigningSignerView;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Milestone;
import tn.esprit.mscontractservicee.entity.SignatureRequest;
import tn.esprit.mscontractservicee.entity.SignatureSigner;
import tn.esprit.mscontractservicee.enums.ContractStatus;
import tn.esprit.mscontractservicee.enums.SignerRole;
import tn.esprit.mscontractservicee.enums.SignatureRequestStatus;
import tn.esprit.mscontractservicee.enums.SignatureSignerStatus;
import tn.esprit.mscontractservicee.enums.SignatureType;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.MilestoneRepository;
import tn.esprit.mscontractservicee.repository.SignatureRequestRepository;
import tn.esprit.mscontractservicee.repository.SignatureSignerRepository;
import tn.esprit.mscontractservicee.service.email.AppEmailService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SignatureRequestServiceImpl implements ISignatureRequestService {

    private static final SecureRandom RNG = new SecureRandom();

    private final ContractRepository contractRepository;
    private final MilestoneRepository milestoneRepository;
    private final SignatureRequestRepository signatureRequestRepository;
    private final SignatureSignerRepository signatureSignerRepository;
    private final IContractService contractService;
    private final AppEmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${app.signature.token-ttl-minutes:2880}")
    private long tokenTtlMinutes;

    private final ObjectMapper snapshotMapper = new ObjectMapper()
            .findAndRegisterModules()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private record SignerWithToken(SignatureSigner signer, String token) {
    }

    @Override
    public SignatureRequestCreateResponse createAndSendForContract(Long contractId) {
        if (contractId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contractId is required");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Contract not found with id: " + contractId));

        if (contract.getStatus() == ContractStatus.DRAFT) {
            contract = contractService.finalizeForSignature(contractId);
        }

        if (contract.getStatus() != ContractStatus.PENDING_SIGNATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract is not ready for signature. Status: " + contract.getStatus());
        }

        // Cancel any previous in-progress request to avoid multiple valid links.
        signatureRequestRepository.findTopByContractIdOrderByIdDesc(contractId).ifPresent(prev -> {
            if (prev.getStatus() == SignatureRequestStatus.CREATED || prev.getStatus() == SignatureRequestStatus.SENT) {
                prev.setStatus(SignatureRequestStatus.CANCELLED);
                signatureRequestRepository.save(prev);
            }
        });

        List<Milestone> milestones = milestoneRepository.findByContractIdOrderByOrdreAsc(contractId);
        if (milestones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request signature without milestones");
        }

        ContractSnapshot snapshot = buildSnapshot(contract, milestones);
        String snapshotJson = toJson(snapshot);
        String snapshotHash = sha256Hex(snapshotJson.getBytes(StandardCharsets.UTF_8));

        SignatureRequest req = SignatureRequest.builder()
                .contractId(contractId)
                .contractVersion(contract.getVersion())
                .snapshotJson(snapshotJson)
                .snapshotHash(snapshotHash)
                .status(SignatureRequestStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
        req = signatureRequestRepository.save(req);

        // Create signers (client + freelancer)
        UserDTO client = contractService.getClientInfo(contractId);
        UserDTO freelancer = contractService.getFreelancerInfo(contractId);

        String clientEmail = requiredEmail(client, "client");
        String freelancerEmail = requiredEmail(freelancer, "freelancer");

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenTtlMinutes);
        SignerWithToken clientSigner = newSignerWithToken(req.getId(), SignerRole.CLIENT, contract.getClientCin(), clientEmail, expiresAt);
        SignerWithToken freelancerSigner = newSignerWithToken(req.getId(), SignerRole.FREELANCER, contract.getFreelancerCin(), freelancerEmail, expiresAt);
        signatureSignerRepository.save(clientSigner.signer());
        signatureSignerRepository.save(freelancerSigner.signer());

        // Send emails
        String subject = "Signature request - Contract " + contract.getReference();
        sendEmail(clientEmail, subject, signerLink(req.getId(), clientSigner.token()), contract);
        sendEmail(freelancerEmail, subject, signerLink(req.getId(), freelancerSigner.token()), contract);

        req.setStatus(SignatureRequestStatus.SENT);
        req.setSentAt(LocalDateTime.now());
        signatureRequestRepository.save(req);

        return SignatureRequestCreateResponse.builder()
                .signatureRequestId(req.getId())
                .status(req.getStatus().name())
                .emailsSent(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SigningRequestViewResponse viewForToken(Long signatureRequestId, String token) {
        SignatureRequest req = signatureRequestRepository.findById(signatureRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Signature request not found with id: " + signatureRequestId));

        SignatureSigner signer = requireSignerByToken(req.getId(), token);
        if (signer.getTokenExpiresAt() != null && signer.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Signing link expired");
        }

        ContractSnapshot snapshot = fromJson(req.getSnapshotJson(), ContractSnapshot.class);
        Contract contract = contractRepository.findById(req.getContractId()).orElse(null);

        List<SignatureSigner> signers = signatureSignerRepository.findBySignatureRequestId(req.getId());
        List<SigningSignerView> signerViews = signers.stream()
                .sorted(Comparator.comparing(s -> s.getRole() != null ? s.getRole().name() : ""))
                .map(s -> SigningSignerView.builder()
                        .role(s.getRole() != null ? s.getRole().name() : null)
                        .email(s.getSignerEmail())
                        .status(s.getStatus() != null ? s.getStatus().name() : null)
                        .signedAt(s.getSignedAt())
                        .build())
                .toList();

        return SigningRequestViewResponse.builder()
                .signatureRequestId(req.getId())
                .contractId(req.getContractId())
                .contractReference(contract != null ? contract.getReference() : null)
                .requestStatus(req.getStatus().name())
                .signerRole(signer.getRole() != null ? signer.getRole().name() : null)
                .signerStatus(signer.getStatus() != null ? signer.getStatus().name() : null)
                .tokenExpiresAt(signer.getTokenExpiresAt())
                .snapshotHash(req.getSnapshotHash())
                .snapshot(snapshot)
                .signers(signerViews)
                .build();
    }

    @Override
    public void sign(Long signatureRequestId, SigningSignRequest request, String ip, String userAgent) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }
        if (request.getSignatureType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signatureType is required");
        }
        if (request.getSignaturePayload() == null || request.getSignaturePayload().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signaturePayload is required");
        }

        SignatureRequest req = signatureRequestRepository.findById(signatureRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Signature request not found with id: " + signatureRequestId));

        if (req.getStatus() == SignatureRequestStatus.CANCELLED || req.getStatus() == SignatureRequestStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE, "Signature request is no longer valid");
        }
        if (req.getStatus() == SignatureRequestStatus.COMPLETED) {
            return; // idempotent
        }

        SignatureSigner signer = requireSignerByToken(req.getId(), request.getToken());
        if (signer.getStatus() == SignatureSignerStatus.SIGNED) {
            return; // idempotent
        }

        if (signer.getTokenExpiresAt() != null && signer.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Signing link expired");
        }

        SignatureType type = request.getSignatureType();
        signer.setSignatureType(type);
        signer.setSignaturePayload(request.getSignaturePayload().trim());
        signer.setDocumentHash(req.getSnapshotHash());
        signer.setIp(ip);
        signer.setUserAgent(userAgent);
        signer.setStatus(SignatureSignerStatus.SIGNED);
        signer.setSignedAt(LocalDateTime.now());
        signatureSignerRepository.save(signer);

        // If all signers signed, complete the request and advance the contract.
        List<SignatureSigner> signers = signatureSignerRepository.findBySignatureRequestId(req.getId());
        boolean allSigned = !signers.isEmpty() && signers.stream().allMatch(s -> s.getStatus() == SignatureSignerStatus.SIGNED);
        if (allSigned) {
            req.setStatus(SignatureRequestStatus.COMPLETED);
            req.setCompletedAt(LocalDateTime.now());
            signatureRequestRepository.save(req);

            Contract contract = contractRepository.findById(req.getContractId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Contract not found with id: " + req.getContractId()));
            if (contract.getDateSignature() == null) {
                contract.setDateSignature(LocalDateTime.now());
            }
            contract.setStatus(ContractStatus.PENDING_PAYMENT);
            contract.setUpdatedAt(LocalDateTime.now());
            contractRepository.save(contract);
        }
    }

    private ContractSnapshot buildSnapshot(Contract contract, List<Milestone> milestones) {
        List<MilestoneSnapshot> ms = milestones.stream()
                .sorted(Comparator.comparing(m -> Optional.ofNullable(m.getOrdre()).orElse(0)))
                .map(m -> MilestoneSnapshot.builder()
                        .milestoneId(m.getId())
                        .ordre(m.getOrdre())
                        .titre(m.getTitre())
                        .description(m.getDescription())
                        .montant(m.getMontant())
                        .deadline(m.getDeadline())
                        .build())
                .toList();

        return ContractSnapshot.builder()
                .contractId(contract.getId())
                .contractVersion(contract.getVersion())
                .reference(contract.getReference())
                .clientCin(contract.getClientCin())
                .freelancerCin(contract.getFreelancerCin())
                .projectId(contract.getProjectId())
                .projectTitle(contract.getProjectTitle())
                .description(contract.getDescription())
                .montantTotal(contract.getMontantTotal())
                .commissionRate(contract.getCommissionRate())
                .slaFreelancerHeures(contract.getSlaFreelancerHeures())
                .slaClientJours(contract.getSlaClientJours())
                .dateDebut(contract.getDateDebut())
                .dateFin(contract.getDateFin())
                .createdAt(contract.getCreatedAt())
                .milestones(ms)
                .build();
    }

    private SignerWithToken newSignerWithToken(Long requestId, SignerRole role, Long cin, String email, LocalDateTime expiresAt) {
        String token = generateToken();
        String tokenHash = sha256Hex(token.getBytes(StandardCharsets.UTF_8));
        SignatureSigner signer = SignatureSigner.builder()
                .signatureRequestId(requestId)
                .role(role)
                .signerCin(cin)
                .signerEmail(email)
                .tokenHash(tokenHash)
                .tokenExpiresAt(expiresAt)
                .status(SignatureSignerStatus.PENDING)
                .build();
        return new SignerWithToken(signer, token);
    }

    private SignatureSigner requireSignerByToken(Long requestId, String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }
        String tokenHash = sha256Hex(token.getBytes(StandardCharsets.UTF_8));
        List<SignatureSigner> signers = signatureSignerRepository.findBySignatureRequestId(requestId);
        return signers.stream()
                .filter(s -> s.getTokenHash() != null && s.getTokenHash().equals(tokenHash))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signing token"));
    }

    private String signerLink(Long requestId, String token) {
        String base = frontendBaseUrl != null ? frontendBaseUrl.replaceAll("/+$", "") : "http://localhost:4200";
        return base + "/sign/contract/" + requestId + "?token=" + token;
    }

    private void sendEmail(String to, String subject, String signingUrl, Contract contract) {
        String body = "Please sign contract " + contract.getReference() + "\n\n"
                + "Open this link to review and sign:\n"
                + signingUrl + "\n\n"
                + "If you did not expect this email, you can ignore it.";
        emailService.sendSignatureRequestEmail(to, subject, body);
    }

    private String requiredEmail(UserDTO user, String label) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing " + label + " email");
        }
        return user.getEmail().trim();
    }

    private String toJson(Object obj) {
        try {
            return snapshotMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize snapshot", e);
        }
    }

    private <T> T fromJson(String json, Class<T> cls) {
        try {
            return snapshotMapper.readValue(json, cls);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse snapshot", e);
        }
    }

    private static String generateToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >>> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
