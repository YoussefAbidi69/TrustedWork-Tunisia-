package tn.esprit.userservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    // ⚠ IMPORTANT: aligné avec frontend
    private String phone;

    //  NOUVEAUX champs (UX premium)
    private String headline;
    private String location;
    private String bio;

    //  chemin image stocké (pas base64)
    private String photo;
}