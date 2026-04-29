package tn.esprit.community.service;

import java.util.List;
import tn.esprit.community.dto.request.CourseReportRequest;
import tn.esprit.community.dto.response.CourseReportResponse;
import tn.esprit.community.entity.enums.ReportStatus;

public interface CourseReportService {
    CourseReportResponse reportCourse(Long courseId, CourseReportRequest reportRequest);

    List<CourseReportResponse> listReportsByCourse(Long courseId);

    CourseReportResponse updateStatus(Long reportId, ReportStatus status);
}
