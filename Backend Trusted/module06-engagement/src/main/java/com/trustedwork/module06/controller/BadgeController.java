package com.trustedwork.module06.controller;

import com.trustedwork.module06.dto.BadgeDTO;
import com.trustedwork.module06.service.BadgeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
@Tag(name = "Badges", description = "Administration des badges")
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ResponseEntity<List<BadgeDTO>> getAll() {
        return ResponseEntity.ok(badgeService.getAllBadges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BadgeDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(badgeService.getBadgeById(id));
    }

    @PostMapping
    public ResponseEntity<BadgeDTO> create(@RequestBody BadgeDTO dto) {
        return ResponseEntity.ok(badgeService.createBadge(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BadgeDTO> update(@PathVariable Long id, @RequestBody BadgeDTO dto) {
        return ResponseEntity.ok(badgeService.updateBadge(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        badgeService.deleteBadge(id);
        return ResponseEntity.ok().build();
    }
}
