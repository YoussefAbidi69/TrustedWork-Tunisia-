package tn.esprit.userservice.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineMemberDTO {
    private Long userId;
    private String firstName;
    private String lastName;
    private String photo;
    private String role; // LEAD | MEMBER | OBSERVER
    private boolean isOnline;
}
