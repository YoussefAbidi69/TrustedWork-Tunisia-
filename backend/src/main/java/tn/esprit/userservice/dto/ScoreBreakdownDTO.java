package tn.esprit.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDTO {
    private Float skillMatch;
    private Float trust;
    private Float availability;
    private Float experience;
    private Float similarity;
    private Float location;
}
