package tn.esprit.freelancerprofileservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.freelancerprofileservice.dto.request.CreateProfileRequest;
import tn.esprit.freelancerprofileservice.dto.request.UpdateProfileRequest;
import tn.esprit.freelancerprofileservice.dto.response.CompletenessResponse;
import tn.esprit.freelancerprofileservice.dto.response.ProfileResponse;
import tn.esprit.freelancerprofileservice.entities.FreelancerProfile;
import tn.esprit.freelancerprofileservice.enums.AvailabilityStatus;
import tn.esprit.freelancerprofileservice.services.ICompletenessService;
import tn.esprit.freelancerprofileservice.services.IFreelancerProfileService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST — gestion des profils freelancer
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class FreelancerProfileController {

    private final IFreelancerProfileService profileService;
    private final ICompletenessService completenessService;

    // POST /api/profiles — créer un profil
    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @Valid @RequestBody CreateProfileRequest request) {

        FreelancerProfile profile = FreelancerProfile.builder()
                .userId(request.getUserId())
                .headline(request.getHeadline())
                .bio(request.getBio())
                .avatarUrl(request.getAvatarUrl())
                .hourlyRate(request.getHourlyRate())
                .availabilityStatus(request.getAvailabilityStatus())
                .visibility(request.getVisibility())
                .projectType(request.getProjectType())
                .region(request.getRegion())
                .build();

        FreelancerProfile saved = profileService.createProfile(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    // GET /api/profiles/user/{userId} — profil par userId
    @GetMapping("/user/{userId}")
    public ResponseEntity<ProfileResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(toResponse(profileService.getByUserId(userId)));
    }

    // GET /api/profiles/{profileId} — profil par profileId
    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> getById(@PathVariable Long profileId) {
        return ResponseEntity.ok(toResponse(profileService.getById(profileId)));
    }

    // PUT /api/profiles/user/{userId} — modifier son profil
    @PutMapping("/user/{userId}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        FreelancerProfile updates = FreelancerProfile.builder()
                .headline(request.getHeadline())
                .bio(request.getBio())
                .avatarUrl(request.getAvatarUrl())
                .hourlyRate(request.getHourlyRate())
                .availabilityStatus(request.getAvailabilityStatus())
                .visibility(request.getVisibility())
                .projectType(request.getProjectType())
                .region(request.getRegion())
                .build();

        return ResponseEntity.ok(toResponse(profileService.updateProfile(userId, updates)));
    }

    // GET /api/profiles — tous les profils publics
    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAllPublic() {
        List<ProfileResponse> profiles = profileService.getAllPublicProfiles()
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(profiles);
    }

    // GET /api/profiles/ranking/{region} — classement régional
    @GetMapping("/ranking/{region}")
    public ResponseEntity<List<ProfileResponse>> getRanking(@PathVariable String region) {
        List<ProfileResponse> profiles = profileService.getRankingByRegion(region)
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(profiles);
    }

    // PATCH /api/profiles/user/{userId}/availability — changer disponibilité
    @PatchMapping("/user/{userId}/availability")
    public ResponseEntity<ProfileResponse> updateAvailability(
            @PathVariable Long userId,
            @RequestParam AvailabilityStatus status) {
        return ResponseEntity.ok(toResponse(profileService.updateAvailability(userId, status)));
    }

    // GET /api/profiles/user/{userId}/completeness — score de complétude
    @GetMapping("/user/{userId}/completeness")
    public ResponseEntity<CompletenessResponse> getCompleteness(@PathVariable Long userId) {
        return ResponseEntity.ok(completenessService.calculateCompleteness(userId));
    }

    // DELETE /api/profiles/user/{userId} — supprimer son profil
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable Long userId) {
        profileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    // Mapper entité → DTO response
    private ProfileResponse toResponse(FreelancerProfile p) {
        return ProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .headline(p.getHeadline())
                .bio(p.getBio())
                .avatarUrl(p.getAvatarUrl())
                .hourlyRate(p.getHourlyRate())
                .availabilityStatus(p.getAvailabilityStatus())
                .visibility(p.getVisibility())
                .projectType(p.getProjectType())
                .completenessScore(p.getCompletenessScore())
                .region(p.getRegion())
                .regionalRank(p.getRegionalRank())
                .totalViews(p.getTotalViews())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}