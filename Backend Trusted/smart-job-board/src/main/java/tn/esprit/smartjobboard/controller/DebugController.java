package tn.esprit.smartjobboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.smartjobboard.config.GoogleCalendarConfig;

import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final GoogleCalendarConfig googleCalendarConfig;

    @GetMapping("/google-auth")
    public ResponseEntity<Map<String, Object>> googleAuth() {
        return ResponseEntity.ok(googleCalendarConfig.getAuthDebugInfo());
    }
}
