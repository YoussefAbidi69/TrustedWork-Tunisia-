package tn.esprit.msprojectservice.services;

import tn.esprit.msprojectservice.dto.ProgressReportDTO;

import java.util.List;

public interface IProgressReportService {

    ProgressReportDTO generateReport(Long projectId);

    ProgressReportDTO getLatestReport(Long projectId);

    List<ProgressReportDTO> getReportHistory(Long projectId);
}