package tn.esprit.mscontractservicee.dto.signing;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SigningSignerView {
    private String role;
    private String email;
    private String status;
    private LocalDateTime signedAt;
}

