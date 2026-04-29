package tn.esprit.smartjobboard.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "ThisIsAVeryLongSecretKeyForTestingPurposesThatIs256BitsLong!!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    private String buildToken(String email, String roles, Integer cin, long expiresInMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        if (cin != null) {
            builder.claim("cin", cin);
        }
        return builder.signWith(key).compact();
    }

    @Nested
    @DisplayName("isTokenValid()")
    class TokenValidity {

        @Test
        @DisplayName("should return true for a valid non-expired token")
        void validToken() {
            String token = buildToken("user@example.com", "FREELANCER", 12345, 600_000);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for an expired token")
        void expiredToken() {
            String token = buildToken("user@example.com", "FREELANCER", 12345, -1000);
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("should return false for a malformed token")
        void malformedToken() {
            assertThat(jwtService.isTokenValid("not.a.valid.jwt")).isFalse();
        }

        @Test
        @DisplayName("should return false for a token signed with different key")
        void wrongKey() {
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "ACompletelyDifferentSecretKeyThatIs256BitsLongForSure!!".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject("user@example.com")
                    .expiration(new Date(System.currentTimeMillis() + 600_000))
                    .signWith(wrongKey)
                    .compact();

            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Claim extraction")
    class ClaimExtraction {

        @Test
        @DisplayName("should extract email from subject")
        void extractEmail() {
            String token = buildToken("dev@example.com", "FREELANCER", null, 600_000);
            assertThat(jwtService.extractEmail(token)).isEqualTo("dev@example.com");
        }

        @Test
        @DisplayName("should extract roles claim")
        void extractRoles() {
            String token = buildToken("dev@example.com", "FREELANCER,ADMIN", null, 600_000);
            assertThat(jwtService.extractRoles(token)).isEqualTo("FREELANCER,ADMIN");
        }

        @Test
        @DisplayName("should return null roles when claim is absent")
        void noRolesClaim() {
            String token = buildToken("dev@example.com", null, null, 600_000);
            assertThat(jwtService.extractRoles(token)).isNull();
        }

        @Test
        @DisplayName("should extract cin claim")
        void extractCin() {
            String token = buildToken("dev@example.com", "FREELANCER", 99887766, 600_000);
            assertThat(jwtService.extractCin(token)).isEqualTo(99887766);
        }

        @Test
        @DisplayName("should extract expiration date")
        void extractExpiration() {
            String token = buildToken("dev@example.com", "FREELANCER", null, 600_000);
            assertThat(jwtService.extractExpiration(token)).isAfter(new Date());
        }
    }

    @Nested
    @DisplayName("isTokenExpired()")
    class TokenExpiry {

        @Test
        @DisplayName("should return false for a future-dated token")
        void notExpired() {
            String token = buildToken("dev@example.com", "FREELANCER", null, 3_600_000);
            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("should return true for a past-dated token via isTokenValid returning false")
        void expired() {
            String token = buildToken("dev@example.com", "FREELANCER", null, -60_000);
            // isTokenValid returns false for expired tokens (catches the exception internally)
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }
    }
}
