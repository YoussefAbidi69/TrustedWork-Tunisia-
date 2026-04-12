package tn.esprit.freelancerprofileservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Utilitaire JWT — valide les tokens émis par le user-service (Module 01)
 * Secret en plain text — aligné sur user-service
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
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
        return claims.get("userId", Long.class);
    }

    /**
     * Extrait le rôle depuis le claim "role" du token JWT.
     * Le user-service stocke le rôle sous la clé "role" (ex: "ADMIN").
     * Retourne "USER" par défaut si le claim est absent.
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        Object role = claims.get("roles");
        return role != null ? role.toString() : "USER";
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}