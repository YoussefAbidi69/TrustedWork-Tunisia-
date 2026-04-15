package tn.esprit.freelancerprofileservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Utilitaire JWT — valide les tokens émis par le user-service (Module 01)
 * Compatible jjwt 0.11.5
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        if (userId == null) return null;
        return Long.valueOf(userId.toString());
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        // user-service stocke le rôle dans "role" (singulier)
        Object role = claims.get("role");
        if (role == null) role = claims.get("roles");
        return role != null ? role.toString() : "USER";
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            // Vérifier expiration manuellement
            return claims.getExpiration().after(new java.util.Date());
        } catch (Exception e) {
            log.error(">>> JWT EXCEPTION détail : {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }
}