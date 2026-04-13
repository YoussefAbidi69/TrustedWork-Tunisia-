package tn.esprit.community.report;

import tn.esprit.community.dto.ReportDTO;

public interface ReportService {
    ReportDTO reportPost(Long reportedBy, Long postId, String reason, String description);
    ReportDTO adminRestorePost(Long postId);
    ReportDTO adminRejectPost(Long postId);
}
