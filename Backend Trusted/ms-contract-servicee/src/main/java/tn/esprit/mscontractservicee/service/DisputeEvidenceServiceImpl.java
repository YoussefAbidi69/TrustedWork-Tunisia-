package tn.esprit.mscontractservicee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.mscontractservicee.dto.dispute.DisputeEvidenceResponse;
import tn.esprit.mscontractservicee.entity.Contract;
import tn.esprit.mscontractservicee.entity.Dispute;
import tn.esprit.mscontractservicee.entity.DisputeEvidence;
import tn.esprit.mscontractservicee.repository.ContractRepository;
import tn.esprit.mscontractservicee.repository.DisputeEvidenceRepository;
import tn.esprit.mscontractservicee.repository.DisputeRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DisputeEvidenceServiceImpl implements IDisputeEvidenceService {

    private final DisputeEvidenceRepository evidenceRepository;
    private final DisputeRepository disputeRepository;
    private final ContractRepository contractRepository;

    @Value("${app.disputes.upload-dir:./uploads/disputes}")
    private String uploadDir;

    @Override
    public DisputeEvidenceResponse uploadEvidence(Long disputeId, Long authenticatedCin, boolean admin, MultipartFile file) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (disputeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disputeId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        Dispute dispute = requireDispute(disputeId);
        Contract contract = requireContract(dispute.getContractId());
        if (!admin) {
            requireParticipant(contract, authenticatedCin);
        }

        String original = safeOriginalFilename(file.getOriginalFilename());
        String stored = UUID.randomUUID().toString().replace("-", "") + "_" + original;

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = root.resolve(String.valueOf(disputeId)).normalize();
        Path dest = dir.resolve(stored).normalize();
        if (!dest.startsWith(dir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        try {
            Files.createDirectories(dir);
            file.transferTo(dest.toFile());
        } catch (Exception e) {
            log.warn("Failed to store dispute evidence. disputeId={} filename={}", disputeId, original, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        Long storedSize;
        try {
            storedSize = Files.size(dest);
        } catch (Exception e) {
            storedSize = file.getSize();
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            try {
                contentType = Files.probeContentType(dest);
            } catch (Exception ignored) {
                contentType = null;
            }
        }

        DisputeEvidence evidence = DisputeEvidence.builder()
                .disputeId(disputeId)
                .uploaderCin(authenticatedCin)
                .originalFilename(original)
                .storedFilename(stored)
                .contentType(contentType)
                .sizeBytes(storedSize)
                .createdAt(LocalDateTime.now())
                .build();
        evidence = evidenceRepository.save(evidence);
        return toResponse(evidence);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeEvidenceResponse> listEvidence(Long disputeId, Long authenticatedCin, boolean admin) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (disputeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disputeId is required");
        }

        Dispute dispute = requireDispute(disputeId);
        Contract contract = requireContract(dispute.getContractId());
        if (!admin) {
            requireParticipant(contract, authenticatedCin);
        }

        return evidenceRepository.findByDisputeIdOrderByCreatedAtDesc(disputeId)
                .stream()
                .map(DisputeEvidenceServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceDownload downloadEvidence(Long disputeId, Long evidenceId, Long authenticatedCin, boolean admin) {
        if (authenticatedCin == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (disputeId == null || evidenceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "disputeId and evidenceId are required");
        }

        Dispute dispute = requireDispute(disputeId);
        Contract contract = requireContract(dispute.getContractId());
        if (!admin) {
            requireParticipant(contract, authenticatedCin);
        }

        DisputeEvidence evidence = evidenceRepository.findByIdAndDisputeId(evidenceId, disputeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not found"));

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(String.valueOf(disputeId)).resolve(evidence.getStoredFilename()).normalize();
        if (!file.startsWith(root.resolve(String.valueOf(disputeId)).normalize())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stored filename");
        }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence file not found on disk");
        }

        Resource resource = new FileSystemResource(file.toFile());
        return new EvidenceDownload(toResponse(evidence), resource);
    }

    private Dispute requireDispute(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispute not found with id: " + disputeId));
    }

    private Contract requireContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found with id: " + contractId));
    }

    private static void requireParticipant(Contract contract, Long cin) {
        if (contract == null || cin == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract or user");
        }
        boolean participant = (contract.getClientCin() != null && contract.getClientCin().equals(cin))
                || (contract.getFreelancerCin() != null && contract.getFreelancerCin().equals(cin));
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a participant of this contract");
        }
    }

    private static DisputeEvidenceResponse toResponse(DisputeEvidence e) {
        return DisputeEvidenceResponse.builder()
                .id(e.getId())
                .disputeId(e.getDisputeId())
                .uploaderCin(e.getUploaderCin())
                .originalFilename(e.getOriginalFilename())
                .contentType(e.getContentType())
                .sizeBytes(e.getSizeBytes())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static String safeOriginalFilename(String raw) {
        String name = (raw == null || raw.isBlank()) ? "file" : raw;
        try {
            name = Paths.get(name).getFileName().toString();
        } catch (Exception ignored) {
            // fall back to raw
        }
        name = name.replace("\\", "_").replace("/", "_").replace("..", "_");
        // keep a conservative set of characters to avoid weird issues on Windows.
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank()) {
            name = "file";
        }
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name;
    }
}
