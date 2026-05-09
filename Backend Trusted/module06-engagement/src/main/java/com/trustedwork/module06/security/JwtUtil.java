package com.trustedwork.module06.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return Jwts.claims();
        }
    }

    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private org.springframework.web.client.RestTemplate loadBalancedRestTemplate;

    private static final String IDENTITY_VALIDATE_URL = "http://user-service/api/identity/validate-token";

    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims == null) return null;
            
            // Priority 1: "id" claim
            Object id = claims.get("id");
            if (id != null) return Long.valueOf(id.toString());
            
            // Priority 2: "userId" claim
            Object userId = claims.get("userId");
            if (userId != null) return Long.valueOf(userId.toString());

            // Priority 3: Fallback - Appeler l'Identity Provider (user-service) pour résoudre l'ID
            try {
                System.out.println("JWT Debug: ID missing in token, calling Identity Provider for session resolution...");
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
                
                java.util.Map response = loadBalancedRestTemplate.postForObject(IDENTITY_VALIDATE_URL, entity, java.util.Map.class);
                if (response != null && response.get("userId") != null) {
                    Long resolvedId = Long.valueOf(response.get("userId").toString());
                    System.out.println("JWT Debug: Successfully resolved userId: " + resolvedId);
                    return resolvedId;
                }
            } catch (Exception e) {
                System.err.println("JWT Debug: Fallback Identity resolution failed: " + e.getMessage());
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}