package com.trustedwork.module06.controller;

import com.trustedwork.module06.dto.LeaderboardDTO;
import com.trustedwork.module06.service.LeaderboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Classement par gouvernorat")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardDTO>> getGlobal() {
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard());
    }

    @GetMapping("/governorate/{gov}")
    public ResponseEntity<List<LeaderboardDTO>> getByGov(@PathVariable String gov) {
        return ResponseEntity.ok(leaderboardService.getLeaderboardByGovernorate(gov));
    }

    @PostMapping("/recompute")
    public ResponseEntity<Void> recompute() {
        leaderboardService.recomputeAllRanks();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/debug")
    public ResponseEntity<String> debug() {
        try {
            leaderboardService.getGlobalLeaderboard();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.status(500).body(sw.toString());
        }
    }
}
