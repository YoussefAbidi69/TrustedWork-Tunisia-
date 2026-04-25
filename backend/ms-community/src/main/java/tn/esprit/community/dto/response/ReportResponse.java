package tn.esprit.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.Enum.ReportStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long postId;
    private Long courseId;
    private Long reportedBy;
    private String reason;
    private String description;
    private ReportStatus status;
}
