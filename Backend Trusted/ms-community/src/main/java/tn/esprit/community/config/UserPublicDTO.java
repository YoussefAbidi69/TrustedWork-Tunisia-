package tn.esprit.community.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicDTO {
    private Long id;
    private String username;
    private String avatarUrl;
}
