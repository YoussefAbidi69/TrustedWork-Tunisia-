package tn.esprit.userservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.dto.UpdateProfileRequest;
import tn.esprit.userservice.dto.UserDTO;
import tn.esprit.userservice.service.IUserService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile management")
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getUserByEmail(authentication.getName()));
    }

    @PutMapping(value = "/{cin}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or isAuthenticated()")
    public ResponseEntity<UserDTO> updateProfile(
            @PathVariable Integer cin,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "headline", required = false) String headline,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPhone(phone);
        request.setHeadline(headline);
        request.setLocation(location);
        request.setBio(bio);

        if (photo != null && !photo.isEmpty()) {
            request.setPhoto(saveProfilePhoto(photo));
        }

        return ResponseEntity.ok(userService.updateProfile(cin, request));
    }

    @GetMapping("/{id}/public")
    public ResponseEntity<PublicUserDTO> getPublicUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getPublicUser(id));
    }

    @GetMapping("/{id}/trust-level")
    public ResponseEntity<?> getTrustLevel(@PathVariable Long id) {
        return ResponseEntity.ok(
                Map.of(
                        "userId", id,
                        "trustLevel", userService.getTrustLevel(id)
                )
        );
    }

    private String saveProfilePhoto(MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_")
                    : "photo.jpg";

            String fileName = "profile_" + UUID.randomUUID() + "_" + originalName;

            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "profiles");
            Files.createDirectories(uploadDir);

            Path destination = uploadDir.resolve(fileName);
            file.transferTo(destination.toFile());

            return "/uploads/profiles/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Erreur upload image", e);
        }
    }

    // SEARCH BY EMAIL EXCLUDING MEMBERS/INVITED
    @GetMapping("/search")
    public ResponseEntity<java.util.List<PublicUserDTO>> searchUsersByEmail(
            @RequestParam String email,
            @RequestParam Long agencyId) {
        
        // Use generic user repository (find by email containing ignoring case)
        // Then filter out members and invited inside the service or controller.
        // For brevity and compliance with requirements, I'll invoke the logic here or pass it to UserService
        return ResponseEntity.ok(userService.searchUsersByEmailExcludingAgency(email, agencyId));
    }
}