package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de réponse — projet portfolio
 */
@Data
@Builder
public class PortfolioResponse {

    private Long id;
    private String title;
    private String description;
    private String projectUrl;
    private String imageUrl;
    private String technologies;
    private LocalDate completionDate;
}