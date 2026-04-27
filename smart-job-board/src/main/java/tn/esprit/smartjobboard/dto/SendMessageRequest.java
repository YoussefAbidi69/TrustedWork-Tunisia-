package tn.esprit.smartjobboard.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {
    private Long jobOfferId;
    private Long receiverId;
    private String content;
}
