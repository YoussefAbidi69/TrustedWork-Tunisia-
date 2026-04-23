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

    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims == null) return null;
            
            // Priority 1: "id" claim (added to main service tokens)
            Object id = claims.get("id");
            if (id != null) {
                return Long.valueOf(id.toString());
            }
            
            // Priority 2: "subject" (if numeric, for backward compatibility)
            String sub = claims.getSubject();
            if (sub != null && sub.matches("\\d+")) {
                return Long.parseLong(sub);
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