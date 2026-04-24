package tn.esprit.freelancerprofileservice.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.freelancerprofileservice.security.JwtAuthFilter;
import tn.esprit.freelancerprofileservice.security.JwtUtil;
import tn.esprit.freelancerprofileservice.services.ExcelExportService;
import tn.esprit.freelancerprofileservice.services.PdfExportService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ExportController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PdfExportService pdfExportService;

    @MockBean
    private ExcelExportService excelExportService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void shouldExportCv() throws Exception {
        byte[] pdfBytes = "PDF_CONTENT".getBytes();
        when(pdfExportService.generateCv(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/export/profiles/1/cv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=cv-freelancer-1.pdf"))
                .andExpect(header().string("Content-Type", "application/pdf"));

        verify(pdfExportService).generateCv(1L);
    }

    @Test
    void shouldExportProfilesExcel() throws Exception {
        byte[] excelBytes = "EXCEL_CONTENT".getBytes();
        when(excelExportService.generateProfilesExcel()).thenReturn(excelBytes);

        mockMvc.perform(get("/api/export/admin/profiles/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=profiles-export.xlsx"));

        verify(excelExportService).generateProfilesExcel();
    }

    @Test
    void shouldExportAdminPdf() throws Exception {
        byte[] pdfBytes = "ADMIN_PDF_CONTENT".getBytes();
        when(pdfExportService.generateAdminReport()).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/export/admin/report/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=admin-report.pdf"))
                .andExpect(header().string("Content-Type", "application/pdf"));

        verify(pdfExportService).generateAdminReport();
    }
}
