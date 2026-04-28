package tn.esprit.userservice.controller.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.userservice.dto.chat.FileUploadResultDTO;
import tn.esprit.userservice.service.ChatFileStorageService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/agencies/{agencyId}/chat")
public class ChatFileController {

    @Autowired
    private ChatFileStorageService storageService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FileUploadResultDTO>> uploadFiles(
            @PathVariable Long agencyId,
            @RequestParam("files") List<MultipartFile> files,
            Principal principal) {
        
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No files provided");
        }
        if (files.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum 5 files allowed");
        }

        List<FileUploadResultDTO> results = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large: " + file.getOriginalFilename());
                }
                FileUploadResultDTO dto = storageService.store(file, agencyId);
                results.add(dto);
            }
        } catch (RuntimeException e) {
            if ("Unsupported Media Type".equals(e.getMessage())) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed");
        }
        
        return ResponseEntity.ok(results);
    }
}
