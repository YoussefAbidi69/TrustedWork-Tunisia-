package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.ReportRequest;
import tn.esprit.community.dto.response.ReportResponse;
import tn.esprit.community.entity.Enum.ReportStatus;

public interface ReportService {
    ReportResponse reportPost(Long postId, ReportRequest reportRequest);

    List<ReportResponse> listReportsByPost(Long postId);

    ReportResponse reportCourse(Long courseId, ReportRequest reportRequest);

    List<ReportResponse> listReportsByCourse(Long courseId);

    ReportResponse updateStatus(Long reportId, ReportStatus status);
}
