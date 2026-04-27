package tn.esprit.userservice.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationLogRequestDto {

    private Long userId;
    private String message;
    private String attachmentUrl;
}
