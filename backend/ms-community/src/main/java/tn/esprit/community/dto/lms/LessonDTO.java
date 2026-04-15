package tn.esprit.community.dto.lms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.community.entity.Enum.LessonType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {
    private Long id;
    private Long sectionId;
    private String title;
    private String content;
    private LessonType type;
    private String videoUrl;
    /** External PDF URL (or uploaded file URL) stored for PDF lessons. */
    private String pdfUrl;
    /** Null on create means append at end. */
    private Integer orderIndex;
}
