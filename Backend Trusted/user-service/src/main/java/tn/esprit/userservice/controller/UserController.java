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
import tn.esprit.userservice.service.CloudinaryService;
import tn.esprit.userservice.service.ISuspensionService;
import tn.esprit.userservice.service.IUserService;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile management")
public class UserController {

    private final IUserService userService;
    private final CloudinaryService cloudinaryService;
    private final ISuspensionService suspensionService;

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
            String uploadedUrl = cloudinaryService.uploadProfilePhoto(photo);
            if (uploadedUrl != null) {
                request.setPhoto(uploadedUrl);
            }
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

    /**
     * POST /users/{userId}/auto-suspend
     * Endpoint interne — appelé par le freelancer-profile-service (IA)
     * pour suspendre automatiquement un compte après 3 avis négatifs.
     * Pas de @PreAuthorize : sécurisé par l'architecture (appel interne uniquement).
     */
    @PostMapping("/{userId}/auto-suspend")
    public ResponseEntity<Void> autoSuspendByAi(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason",
                    "Suspension automatique IA — avis négatifs répétés");
            suspensionService.suspendUser(userId, reason, "system-ai");
        } catch (IllegalStateException e) {
            // Déjà suspendu — on ignore silencieusement
        }
        return ResponseEntity.noContent().build();
    }

}