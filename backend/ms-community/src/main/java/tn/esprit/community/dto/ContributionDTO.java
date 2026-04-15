package tn.esprit.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributionDTO {
    private Long id;
    private Long userId;
    private int sharedCourseCount;
}
