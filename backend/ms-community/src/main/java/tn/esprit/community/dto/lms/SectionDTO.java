package tn.esprit.community.dto.lms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {
    private Long id;
    private Long courseId;
    private String title;
    /** Null on create means append at end. */
    private Integer orderIndex;
}
