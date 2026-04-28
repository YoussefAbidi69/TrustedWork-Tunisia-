package tn.esprit.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationFilterDTO {
    private Float minScore;
    private String skills; // comma-separated
    private String availability;
    private String sortBy; // score, trust, availability
    private String search;
    private boolean refresh;
}
