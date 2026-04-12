package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de réponse — expérience professionnelle
 */
@Data
@Builder
public class WorkExperienceResponse {

    private Long id;
    private String jobTitle;
    private String company;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}