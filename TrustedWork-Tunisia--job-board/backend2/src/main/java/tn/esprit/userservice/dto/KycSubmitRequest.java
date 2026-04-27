package tn.esprit.userservice.dto;

import lombok.Data;

@Data
public class KycSubmitRequest {
    private String cinNumber;
    private String cinDocumentPath;
    private String selfiePath;
    private String diplomaDocumentPath;
}