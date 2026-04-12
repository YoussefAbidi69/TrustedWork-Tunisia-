package tn.esprit.msprojectservice.services;

public interface IExportService {

    byte[] exportProjectReportPdf(Long projectId);

    byte[] exportProjectTasksCsv(Long projectId);
}