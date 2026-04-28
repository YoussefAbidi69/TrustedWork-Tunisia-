package tn.esprit.userservice.dto.chat;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResultDTO {
    private String url;
    private String filename;
    private String fileType;
    private long fileSize;
}
