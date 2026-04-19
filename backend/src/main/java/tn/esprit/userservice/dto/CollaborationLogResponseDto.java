package tn.esprit.userservice.dto;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationLogResponseDto {

    private Long id;
    private Long agencyId;
    private Long userId;
    private String message;
    private String attachmentUrl;
    private LocalDateTime sentAt;
}