package tn.esprit.freelancerprofileservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private final String secret = "mysecretmysecretmysecretmysecret";

    @BeforeEach
    void setup() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", secret);
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String generateToken() {
        return Jwts.builder()
                .setSubject("user@test.com")
                .claim("userId", 1L)
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(signingKey())
                .compact();
    }

    @Test
    void shouldExtractUsername() {
        String token = generateToken();

        String username = jwtUtil.extractUsername(token);

        assertNotNull(username);
        assertEquals("user@test.com", username);
    }

    @Test
    void shouldExtractUserId() {
        String token = generateToken();

        Long userId = jwtUtil.extractUserId(token);

        assertNotNull(userId);
        assertEquals(1L, userId);
    }

    @Test
    void shouldExtractRole() {
        String token = generateToken();

        String role = jwtUtil.extractRole(token);

        assertNotNull(role);
        assertEquals("ADMIN", role);
    }

    @Test
    void shouldValidateToken() {
        String token = generateToken();

        boolean isValid = jwtUtil.isTokenValid(token);

        assertTrue(isValid);
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        boolean isValid = jwtUtil.isTokenValid("invalid.token");

        assertFalse(isValid);
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        String expiredToken = Jwts.builder()
                .setSubject("user@test.com")
                .claim("userId", 1L)
                .claim("role", "ADMIN")
                .setIssuedAt(new Date(System.currentTimeMillis() - 200000))
                .setExpiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(signingKey())
                .compact();

        boolean isValid = jwtUtil.isTokenValid(expiredToken);

        assertFalse(isValid);
    }
}