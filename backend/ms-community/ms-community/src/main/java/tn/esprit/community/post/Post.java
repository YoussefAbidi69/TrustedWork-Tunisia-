package tn.esprit.community.post;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    @Enumerated(EnumType.STRING)
    private PostType type;
    private String mediaUrl;
    private String fileUrl;
    private Long createdBy;
    private Long communityId;
    @Enumerated(EnumType.STRING)
    private PostStatus status;
    private boolean isAiGenerated;
    private boolean isValidated;
    private int reportCount;
}
