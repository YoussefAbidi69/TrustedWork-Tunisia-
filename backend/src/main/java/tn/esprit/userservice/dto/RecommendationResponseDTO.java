package tn.esprit.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDTO {
    private Long agencyId;
    private String agencyName;
    private Integer totalCandidates;
    private Integer page;
    private Integer size;
    private List<FreelancerRecommendationDTO> recommendations;
}
