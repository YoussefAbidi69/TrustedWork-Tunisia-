package tn.esprit.mscontractservicee.dto.dispute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEvidenceResponse {
    private Long id;
    private Long disputeId;
    private Long uploaderCin;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}

