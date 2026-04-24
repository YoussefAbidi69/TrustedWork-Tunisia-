package tn.esprit.freelancerprofileservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de réponse — projet portfolio (version premium)
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

    private boolean pinned;

    //  score de complétude du projet (0 - 100)
    private int projectScore;
}